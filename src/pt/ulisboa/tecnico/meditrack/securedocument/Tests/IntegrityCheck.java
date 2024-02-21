package pt.ulisboa.tecnico.meditrack.securedocument.Tests;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import pt.ulisboa.tecnico.meditrack.securedocument.Protect;
import pt.ulisboa.tecnico.meditrack.securedocument.Check;
import pt.ulisboa.tecnico.meditrack.securedocument.Common;

public class IntegrityCheck {
    
    public static JsonObject createJson() {
        JsonObject json = new JsonObject();

        JsonObject generalData = new JsonObject();
        JsonObject generalContents = new JsonObject();

        generalContents.addProperty("name", "Nina");
        generalContents.addProperty("sex", "Male");
        generalContents.addProperty("date", "2023-12-21");
        generalContents.addProperty("bloodType", "A+");
        JsonArray allergies = new JsonArray();
        allergies.add("Penincilin");
        generalContents.add("knownAllergies", allergies);

        generalData.add("contents", generalContents);
        JsonArray generalMetadata = new JsonArray();

        generalData.add("contents", generalContents);
        generalData.add("metadata", generalMetadata);

        json.add("General Data", generalData);

        JsonObject newSection = new JsonObject();
        JsonObject sectionContents = new JsonObject();

        JsonObject consultation = new JsonObject();
        consultation.addProperty("date", "2023-12-21");
        consultation.addProperty("speciality", "Dermathology");
        consultation.addProperty("doctor", "Dr. Jordan");
        consultation.addProperty("practice", "Clinic");
        consultation.addProperty("summary", "Prescribed some cream");

        JsonArray consultations = new JsonArray();
        consultations.add(consultation);
        sectionContents.add("consultations", consultations);
        newSection.add("contents", sectionContents);

        JsonArray sectionMetadata = new JsonArray();
        newSection.add("metadata", sectionMetadata);

        json.add("Dermathology", newSection);

        return json;
    }

    public static void modifyFile() throws Exception {
        // We read the encrypted original JSON
        JsonObject json = Common.readJSON("src/pt/ulisboa/tecnico/meditrack/securedocument/Tests/encryptedTest.json");
        // We modify the contents of "General Data"
        String contents = json.getAsJsonObject("General Data").get("contents").getAsString();
        char[] charContents = contents.toCharArray();
        charContents[10] = (char) (charContents[10] + 1);
        contents = new String(charContents);
        json.getAsJsonObject("General Data").addProperty("contents", contents);
        // We write it to another file to show the difference
        Common.writeJSON(json, "src/pt/ulisboa/tecnico/meditrack/securedocument/Tests/changedEncryptedTest.json");
    }

    public static void main(String[] args) {
        
        // This test requires access to public keys of emergency and Nina, along with emergency's private key.
        
        JsonObject json = createJson();
        try {
            Common.writeJSON(json, "src/pt/ulisboa/tecnico/meditrack/securedocument/Tests/clearTest.json");
            // We'll add the metadata for emergency and Nina, who is the owner.
            Protect.encryptJSON(json, "emergency", "Nina");
            Common.writeJSON(json, "src/pt/ulisboa/tecnico/meditrack/securedocument/Tests/encryptedTest.json");
            modifyFile();
            json = Common.readJSON("src/pt/ulisboa/tecnico/meditrack/securedocument/Tests/changedEncryptedTest.json");
            System.out.println("Checking integrity of JSON...");
            Check.checkJSON(json, "emergency");
        } catch (Exception e) {
            System.err.println("Error: " + e);
            return;
        }
    }
}
