package me.a06.meditrack.service;

import me.a06.meditrack.securedocument.Common;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.a06.meditrack.domain.PatientData;
import me.a06.meditrack.repository.PatientDataRepository;
import me.a06.meditrack.securedocument.Protect;
import me.a06.meditrack.securedocument.Unprotect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

@Service
public class PatientDataService {

    @Autowired
    PatientDataRepository patientDataRepository;

    @Autowired
    private List<String> uuidList;

    public String readPatientData(String payload) {
        JsonObject request = JsonParser.parseString(payload).getAsJsonObject();

        if (!request.get("command").getAsString().equalsIgnoreCase("read")) return "ERROR: Command is not a read command.";

        String sender = request.getAsJsonObject("info").get("sender").getAsString();
        String patient = request.getAsJsonObject("info").get("patient").getAsString();
        String uuid = request.get("uuid").getAsString();
        if(uuidList.contains(uuid)) return "ERROR: UUID is not unique.";
        uuidList.add(uuid);
        String signature = request.get("signature").getAsString();

        JsonObject json = new JsonObject();
        json.addProperty("command", "read");
        JsonObject messageInfo = new JsonObject();
        messageInfo.addProperty("sender", sender);
        messageInfo.addProperty("patient", patient);
        json.add("info", messageInfo);
        json.addProperty("uuid", uuid);

        byte[] receivedSignature = Common.decodeBase64(signature);
        byte[] senderPublicKey;
        boolean isSignatureValid;

        try {
            senderPublicKey = Common.readPubKey(sender);
            isSignatureValid = Common.verifySignature(new Gson().toJson(json).getBytes(), receivedSignature, senderPublicKey);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: public key not found.";
        }

        if(!isSignatureValid) return "ERROR: signature not valid";

        if(!patientDataRepository.findById(patient).isPresent()) return "ERROR: patient not found.";

        return patientDataRepository.findById(patient).get().getDocument().toString();
    }

    public JsonObject getSection(String name, String sectionName) {
        String documentJson = patientDataRepository.findById(name)
                .orElseThrow()
                .getDocument()
                .toString();

        JsonObject document = JsonParser.parseString(documentJson).getAsJsonObject();

        return document.getAsJsonObject(sectionName);
    }

    public boolean hasPermission(String patient, String client, String sectionName) {
        JsonObject section = getSection(patient, sectionName);

        if (section != null) {
            JsonArray keysArray = section.getAsJsonArray("metadata");

            if (keysArray != null) {
                for (int i = 0; i < keysArray.size(); i++) {
                    JsonObject keyObject = keysArray.get(i).getAsJsonObject();
                    String clientValue = keyObject.getAsJsonPrimitive("client").getAsString();

                    if (client.equalsIgnoreCase(clientValue)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public String registerNewConsultation(String name, String date, String medicalSpeciality, String doctorName, String practice, String treatmentSummary, String cSignature) {

        if(!patientDataRepository.findById(name).isPresent()) return "ERROR: Patient not found.";

        PatientData patientData = patientDataRepository.findById(name).get();

        JsonObject document = patientData.getDocument();

        try {
            Unprotect.decryptJSON(document, "emergency");
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: Decrypting Json after fetching from database failed.";
        }

        JsonObject speciality = document.getAsJsonObject(medicalSpeciality);

        if (speciality == null) {
            speciality = new JsonObject();
            document.add(medicalSpeciality, speciality);
        }

        JsonObject content = speciality.getAsJsonObject("contents");

        if (content == null) {
            content = new JsonObject();
            JsonArray metadata = new JsonArray();
            speciality.add("contents", content);
            speciality.add("metadata", metadata);
        }

        JsonArray consultations = content.getAsJsonArray("consultations");

        if (consultations == null) {
            consultations = new JsonArray();
            content.add("consultations", consultations);
        }

        JsonObject newConsultation = new JsonObject();
        newConsultation.addProperty("date", date);
        newConsultation.addProperty("medicalSpeciality", medicalSpeciality);
        newConsultation.addProperty("doctorName", doctorName);
        newConsultation.addProperty("practice", practice);
        newConsultation.addProperty("treatmentSummary", treatmentSummary);
        newConsultation.addProperty("consultationSignature", cSignature);

        consultations.add(newConsultation);

        try {
            Protect.encryptJSON(document, "emergency", name);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: Encrypting Json after changes failed.";
        }

        patientData.setJsondocument(document.toString());
        patientDataRepository.save(patientData);
        return "OK";
    }

    public String registerConsultation(String payload) {
        JsonObject request = JsonParser.parseString(payload).getAsJsonObject();
        String command = request.get("command").getAsString();

        if (!"register".equalsIgnoreCase(command)) return "ERROR: Command is not a register command.";

        JsonObject info = request.getAsJsonObject("info");
        String sender = info.get("sender").getAsString();
        String patient = info.get("patient").getAsString();
        JsonObject consultation = info.getAsJsonObject("consultation");
        String consultationSignature = info.get("consultationSignature").getAsString();

        byte[] receivedCSignature = Common.decodeBase64(consultationSignature);
        byte[] senderPublicKey;
        boolean isCSignatureValid;

        JsonObject jsonForCSignature = new JsonObject();
        jsonForCSignature.addProperty("date", consultation.get("date").getAsString());
        jsonForCSignature.addProperty("speciality", consultation.get("speciality").getAsString());
        jsonForCSignature.addProperty("doctor", consultation.get("doctor").getAsString());
        jsonForCSignature.addProperty("practice", consultation.get("practice").getAsString());
        jsonForCSignature.addProperty("summary", consultation.get("summary").getAsString());

        try {
            senderPublicKey = Common.readPubKey(sender);
            isCSignatureValid = Common.verifySignature(new Gson().toJson(jsonForCSignature).getBytes(), receivedCSignature, senderPublicKey);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: public key not found or signature verification failed.";
        }

        String uuid = request.get("uuid").getAsString();
        if(uuidList.contains(uuid)) return "ERROR: UUID is not unique.";
        uuidList.add(uuid);
        String signature = request.get("signature").getAsString();

        if (!isCSignatureValid) return "ERROR: Signature not valid";

        // Preparing data for signature verification
        JsonObject jsonForSignature = new JsonObject();
        jsonForSignature.addProperty("command", "register");
        jsonForSignature.add("info", info);
        jsonForSignature.addProperty("uuid", uuid);

        byte[] receivedSignature = Common.decodeBase64(signature);
        boolean isSignatureValid;

        try {
            senderPublicKey = Common.readPubKey(sender);
            isSignatureValid = Common.verifySignature(new Gson().toJson(jsonForSignature).getBytes(), receivedSignature, senderPublicKey);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: public key not found or signature verification failed.";
        }

        if (!isSignatureValid) return "ERROR: Signature not valid";

        // Extracting consultation details
        String date = consultation.get("date").getAsString();
        String speciality = consultation.get("speciality").getAsString();
        String doctor = consultation.get("doctor").getAsString();
        String practice = consultation.get("practice").getAsString();
        String summary = consultation.get("summary").getAsString();
        String cSignature = info.get("consultationSignature").getAsString();

        // Register the new consultation
        return registerNewConsultation(patient, date, speciality, doctor, practice, summary, cSignature);
    }

    // Grant on an existing client does nothing
    public int grantPermission(JsonObject json, String target, String sectionName) throws IOException, FileNotFoundException,
    IllegalStateException, ClassCastException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {

        try {
            Unprotect.decryptJSON(json, "emergency");
        } catch (Exception e) {
            e.printStackTrace();
        }

        JsonObject section = json.getAsJsonObject(sectionName);
        String patientName = json.get("General Data").getAsJsonObject().get("contents").getAsJsonObject().get("name").getAsString();
        if (section == null) {
            return 1;
        }
        JsonArray metadata = section.getAsJsonArray("metadata");
        if (metadata == null) {
            return 2;
        }

        String encodedKey = null;
        String encodedIV = null;

        int size = metadata.size();
        for (int i = 0; i < size; i++) {
            JsonObject encryptionData = metadata.get(i).getAsJsonObject();
            String jsonClient = encryptionData.get("client").getAsString();
            if (jsonClient.equalsIgnoreCase("emergency")) {
                encodedKey = encryptionData.get("key").getAsString();
                encodedIV = encryptionData.get("IV").getAsString();
            }
            if (jsonClient.equalsIgnoreCase(target)) {
                return 3; // The target already has permissions}
            }
        }
        if (encodedKey == null || encodedIV == null) {
            return 4;
        }

        JsonObject newClientData = new JsonObject();
        newClientData.addProperty("client", target);

        byte[] privEmergencyKey = Common.readPrivKey("emergency");

        byte[] symKey = Common.asymDecrypt(Common.decodeBase64(encodedKey), privEmergencyKey);
        byte[] IV = Common.asymDecrypt(Common.decodeBase64(encodedIV), privEmergencyKey);

        byte[] pubTargetKey;

        try {
            pubTargetKey = Common.readPubKey(target);
        } catch (Exception e) {
            e.printStackTrace();
            return 6;
        }

        byte[] encryptedTargetSymKey = Common.asymEncrypt(symKey, pubTargetKey);
        byte[] encryptedTargetIV = Common.asymEncrypt(IV, pubTargetKey);

        newClientData.addProperty("key", Common.encodeBase64(encryptedTargetSymKey));
        newClientData.addProperty("IV", Common.encodeBase64(encryptedTargetIV));
        PatientData patient = patientDataRepository.findById(patientName).get();
        JsonObject jsonobj = patient.getDocument().getAsJsonObject();
        jsonobj.get(sectionName).getAsJsonObject().get("metadata").getAsJsonArray().add(newClientData);
        try {
            Protect.encryptJSON(jsonobj, "emergency", patientName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        patient.setJsondocument(jsonobj.toString());
        patientDataRepository.save(patient);
        return 0;
    }

    // Revoke on a non-existing client, regenerates keys for everyone
    public int revokePermission(JsonObject json, String target, String sectionName, String patientName) throws IllegalStateException, ClassCastException,
            NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, BadPaddingException,
            IllegalBlockSizeException, InvalidKeySpecException, IOException {

        if (target.equalsIgnoreCase("emergency")) return 5;
        JsonObject section = json.getAsJsonObject(sectionName);
        if (section == null) return 1;
        JsonArray metadata = section.getAsJsonArray("metadata");
        if (metadata == null) return 2;

        JsonObject sectionWrapper = new JsonObject();
        sectionWrapper.add(sectionName, section);
        try {
            Unprotect.decryptJSON(sectionWrapper, "emergency");
        } catch (Exception e) {
            e.printStackTrace();
        }

        section = sectionWrapper.getAsJsonObject(sectionName);

        byte[] symKey = Common.generateKey().getEncoded();
        byte[] IV = Common.generateIV();

        int size = metadata.size();

        int index = -1;
        for (int i = 0; i < size; i++) {
            JsonObject encryptionData = metadata.get(i).getAsJsonObject();
            String jsonClient = encryptionData.get("client").getAsString();
            if (jsonClient.equalsIgnoreCase(target)) {
                index = i; // Remove the target
            }
            else {
                String encryptionClient = encryptionData.get("client").getAsString();

                byte[] pubClientKey = Common.readPubKey(encryptionClient);

                byte[] encryptedClientSymKey = Common.asymEncrypt(symKey, pubClientKey);
                byte[] encryptedClientIV = Common.asymEncrypt(IV, pubClientKey);

                encryptionData.addProperty("key", Common.encodeBase64(encryptedClientSymKey));
                encryptionData.addProperty("IV", Common.encodeBase64(encryptedClientIV));
            }
        }
        if(index >= 0) metadata.remove(index);

        sectionWrapper = new JsonObject();
        sectionWrapper.add(sectionName, section);
        PatientData patient = patientDataRepository.findById(patientName).get();
        try {
            Protect.encryptJSON(sectionWrapper, "emergency", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        section = sectionWrapper.getAsJsonObject(sectionName);
        json.add(sectionName, section);
        patient.setJsondocument(json.toString());
        patientDataRepository.save(patient);
        return 0;
    }

    public String changePermission(String payload) {
        JsonObject request = JsonParser.parseString(payload).getAsJsonObject();
        String command = request.get("command").getAsString();

        if (!"change".equalsIgnoreCase(command)) return "ERROR: Command is not a change command.";

        JsonObject info = request.getAsJsonObject("info");
        String sender = info.get("sender").getAsString();
        String doctor = info.get("doctor").getAsString();
        String action = info.get("action").getAsString();
        JsonArray sections = info.getAsJsonArray("sections");
        String uuid = request.get("uuid").getAsString();
        if(uuidList.contains(uuid)) return "ERROR: UUID is not unique.";
        uuidList.add(uuid);
        String signature = request.get("signature").getAsString();

        // Preparing data for signature verification
        JsonObject jsonForSignature = new JsonObject();
        jsonForSignature.addProperty("command", "change");
        jsonForSignature.add("info", info);
        jsonForSignature.addProperty("uuid", uuid);

        byte[] receivedSignature = Common.decodeBase64(signature);
        byte[] senderPublicKey;
        boolean isSignatureValid;

        try {
            senderPublicKey = Common.readPubKey(sender);
            isSignatureValid = Common.verifySignature(new Gson().toJson(jsonForSignature).getBytes(), receivedSignature, senderPublicKey);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: public key not found or signature verification failed.";
        }

        if (!isSignatureValid) return "ERROR: Signature not valid";

        int result = -1;

        if(action.equalsIgnoreCase("grant")) {
            for(int i = 0; i < sections.size(); i++) {
                try {
                    JsonObject patientjson = patientDataRepository.findById(sender).get().getDocument();
                    result = grantPermission(patientjson, doctor, sections.get(i).getAsString());
                } catch (Exception e) {
                    e.printStackTrace();
                    return "ERROR: grant permission failed.";
                }
            }
        } else if (action.equalsIgnoreCase("revoke")) {
            for(int i = 0; i < sections.size(); i++) {
                try {
                    JsonObject patientjson = patientDataRepository.findById(sender).get().getDocument();
                    result = revokePermission(patientjson, doctor, sections.get(i).getAsString(), sender);
                } catch (Exception e) {
                    e.printStackTrace();
                    return "ERROR: revoke permission failed.";
                }
            }
        } else {
            return "ERROR: action not recognized.";
        }

        switch(result) {
            case 0:
                return "OK";
            case 1:
                return "ERROR: Invalid section.";
            case 2:
                return "ERROR: Invalid metadata.";
            case 3:
                return "ERROR: Target already has permissions.";
            case 4:
                return "ERROR: Encoded key or iv null.";
            case 5:
                return "ERROR: Target can't be emergency.";
            case 6:
                return "ERROR: Doctor doesn't exist.";
            default:
                return "ERROR: Something unexpected happened.";
        }
    }


}
