package me.a06.meditrack.securedocument;

import me.a06.meditrack.securedocument.Exceptions.ClientNotFoundException;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.BadPaddingException;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

/*
 * Protects the input, generating a new file after 
 * enforcing confidentiality and integrity.
 */
public class Protect {

    public static String getPatientName(JsonObject json) throws ClassCastException, IllegalStateException, UnsupportedOperationException {
        JsonObject generalData = json.getAsJsonObject("General Data");
        if (generalData == null) return null;
        JsonObject contents = generalData.getAsJsonObject("contents");
        if (contents == null) return null;
        return contents.get("name").getAsString();
    } 

    public static void encryptJSON(JsonObject json, String client, String patient) throws NoSuchAlgorithmException, NoSuchPaddingException,
    InvalidKeyException, InvalidAlgorithmParameterException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException,
    FileNotFoundException, IOException, ClientNotFoundException, ClassCastException, IllegalStateException, UnsupportedOperationException {

        for (Map.Entry<String, JsonElement> jsonEntry : json.entrySet()) {
            JsonObject section = jsonEntry.getValue().getAsJsonObject() ; // Contents + metadata
            JsonArray metadata = section.get("metadata").getAsJsonArray(); // Metadata
            JsonElement contentsTest = section.get("contents"); // Contents
            // If the section is not a JsonObject or a JsonArray it means it's already encrypted so we can skip it
            if (!contentsTest.isJsonObject() && !contentsTest.isJsonArray()) continue;
            byte[] contents = contentsTest.toString().getBytes();

            int size = metadata.size();
            if (size == 0) {
                System.out.println("New section " + jsonEntry.getKey() + " detected. Creating...");
                setupNewSection(section, patient);
                continue;
            }
            boolean foundKeys = false;
            System.out.println("Found metadata. Initiating search for keys...");
            for (int i = 0; i < size; i++) {
                JsonObject encryptionData = metadata.get(i).getAsJsonObject();
                String jsonClient = encryptionData.get("client").getAsString();
                if (!jsonClient.toLowerCase().equals(client.toLowerCase())) continue;

                // Both key and IV fields are encoded in Base64
                byte[] encryptedKey = Common.decodeBase64(encryptionData.get("key").getAsString());
                byte[] encryptedIV = Common.decodeBase64(encryptionData.get("IV").getAsString());

                byte[] privKey = Common.readPrivKey(client.toLowerCase());

                byte[] symKey = Common.asymDecrypt(encryptedKey, privKey);
                byte[] IV = Common.asymDecrypt(encryptedIV, privKey);

                byte[] encrypted = encrypt(contents, symKey, IV);
                section.addProperty("contents", Common.encodeBase64(encrypted));
                // We already found the corresponding client's key
                foundKeys = true;
                break;
            }
            if (!foundKeys) throw new ClientNotFoundException(client);
        }
    }

    public static byte[] encrypt(byte[] toEncrypt, byte[] key, byte[] IV) throws NoSuchAlgorithmException, NoSuchPaddingException,
    InvalidKeyException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, IV));
        cipher.updateAAD("meditrack".getBytes());
        byte[] encryptedJson = cipher.doFinal(toEncrypt);
        return encryptedJson;
    }

    public static void setupNewSection(JsonObject section, String patientName) throws NoSuchAlgorithmException, 
    NoSuchPaddingException,  InvalidKeyException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, 
    InvalidKeySpecException, FileNotFoundException, IOException, IllegalStateException {
        byte[] contents = section.get("contents").toString().getBytes();
        byte[] symKey = Common.generateKey().getEncoded();
        byte[] IV = Common.generateIV();
        byte[] encrypted = encrypt(contents, symKey, IV);

        section.addProperty("contents", Common.encodeBase64(encrypted));

        JsonArray metadata = section.get("metadata").getAsJsonArray(); // Metadata
        
        try {
            // Get emergency public key
            byte[] pubKey = Common.readPubKey("emergency");
            byte[] emergencyEncryptedKey = Common.asymEncrypt(symKey, pubKey);
            byte[] emergencyEncryptedIV = Common.asymEncrypt(IV, pubKey);
            JsonObject emergency = new JsonObject();
            emergency.addProperty("client", "emergency");
            emergency.addProperty("key", Common.encodeBase64(emergencyEncryptedKey));
            emergency.addProperty("IV", Common.encodeBase64(emergencyEncryptedIV));
            metadata.add(emergency);
        } catch (FileNotFoundException e) {
            System.err.println("Public key file for emergency not found");
        } catch (IOException e) {
            System.err.println("Issue reading key from file");
        }

        try {
            // Get the patient's public key
            byte[] pubKey = Common.readPubKey(patientName.toLowerCase());
            byte[] patientEncryptedKey = Common.asymEncrypt(symKey, pubKey);
            byte[] patientEncryptedIV = Common.asymEncrypt(IV, pubKey);
            JsonObject patient = new JsonObject();
            patient.addProperty("client", patientName);
            patient.addProperty("key", Common.encodeBase64(patientEncryptedKey));
            patient.addProperty("IV", Common.encodeBase64(patientEncryptedIV));
            metadata.add(patient);
        } catch (FileNotFoundException e) {
            System.err.println("Public key file for " + patientName + " not found");
        } catch (IOException e) {
            System.err.println("Issue reading key from file");
        }
    }

    public static void main(String[] args) {

        if (args.length < 3) {
            System.err.println("Arguments missing!");
            System.err.println("Usage: protect (input-file) (output-file) (client)");
            return;
        }

        final String inputFile = args[0];
        final String outputFile = args[1];
        final String client = args[2];

        try {
            JsonObject json = Common.readJSON(inputFile);
            encryptJSON(json, client, client);
            Common.writeJSON(json, outputFile);
            System.out.println("File successfully encrypted and written to " + outputFile);            
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            System.err.println("Invalid cipher instance");
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            System.err.println("Issue with symmetric decryption: " + e);
        } catch (InvalidKeySpecException e) {
            System.err.println("Issue with asymmetric decryption: " + e);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            System.err.println("Issue decrypting file: " + e);
        } catch (FileNotFoundException e) {
            System.err.printf("File was not found " + e);
        } catch (IOException e) {
            System.err.println("Issue reading input: " + e);
        } catch (ClientNotFoundException e) {
            System.err.println("Could not cipher document. " + e.getMessage());
        } catch (IllegalStateException | ClassCastException | UnsupportedOperationException e) {
            System.err.println("Issues with JSON handling: " + e.getMessage());
        }
    }
}
