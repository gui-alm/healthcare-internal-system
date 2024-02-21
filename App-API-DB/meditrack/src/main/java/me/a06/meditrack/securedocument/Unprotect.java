package me.a06.meditrack.securedocument;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import java.security.NoSuchAlgorithmException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

import java.util.Map;

/*
 * Unprotects the input, generating a new file after 
 * removing the security from the input.
 */
public class Unprotect {

    public static void decryptJSON(JsonObject json, String client) throws NoSuchAlgorithmException, NoSuchPaddingException,
    InvalidKeyException, InvalidAlgorithmParameterException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, 
    FileNotFoundException, IOException, IllegalStateException, UnsupportedOperationException {
        for (Map.Entry<String, JsonElement> jsonEntry : json.entrySet()) {
            JsonObject section = jsonEntry.getValue().getAsJsonObject(); // Contents + metadata
            JsonArray metadata = section.get("metadata").getAsJsonArray(); // Metadata
            JsonElement test = section.get("contents");
            // If, for some reason, the section is not encrypted aka is in JSON format
            if (test.isJsonObject() || test.isJsonArray()) continue;
            byte[] contents = Common.decodeBase64(test.getAsString()); // Contents

            int size = metadata.size();
            boolean keysFound = false;
            try {
                for (int i = 0; i < size; i++) {
                    JsonObject encryptionData = metadata.get(i).getAsJsonObject();
                    String jsonClient = encryptionData.get("client").getAsString();
                    if (!jsonClient.toLowerCase().equals(client.toLowerCase())) continue;
                    
                    // Both key and IV fields are encoded in Base64
                    byte[] encryptedKey = Common.decodeBase64(encryptionData.get("key").getAsString());
                    byte[] encryptedIV = Common.decodeBase64(encryptionData.get("IV").getAsString());
                    
                    byte[] privKey = Common.readPrivKey(client);
                    
                    byte[] symKey = Common.asymDecrypt(encryptedKey, privKey);
                    byte[] IV = Common.asymDecrypt(encryptedIV, privKey);
                    
                    byte[] decrypted = Common.decrypt(contents, symKey, IV);
                    String jsonString = new String(decrypted);
                    section.add("contents", JsonParser.parseString(jsonString).getAsJsonObject());
                    // We already found the corresponding client's key
                    keysFound = true;
                    break;
                }
            } catch (FileNotFoundException e) {
                System.err.println("Found the user, but it's not you...");
            }
            if (!keysFound) System.out.println("Didn't decipher " + jsonEntry.getKey() + ", since you have no permission to read it...");
        }
    }

    public static void main(String[] args) {

        if (args.length < 3) {
            System.err.println("Arguments missing!");
            System.err.println("Usage: unprotect (input-file) (output-file) (client)");
            return;
        }

        final String inputFile = args[0];
        final String outputFile = args[1];
        final String client = args[2];

        try {
            JsonObject json = Common.readJSON(inputFile);
            decryptJSON(json, client);
            Common.writeJSON(json, outputFile);
            System.out.println("Decrypted file written to " + outputFile + " successfully");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            System.err.println("Invalid cipher instance");
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            System.err.println("Issue with symmetric decryption: " + e.getMessage());
        } catch (InvalidKeySpecException e) {
            System.err.println("Issue with asymmetric decryption: " + e.getMessage());
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            System.err.println("Issue decrypting file: " + e.getMessage());
        } catch (FileNotFoundException e) {
            System.err.println("Private key file not found");
        } catch (IOException e) {
            System.err.println("Issue reading key from file");
        } catch (IllegalStateException | UnsupportedOperationException e) {
            System.err.println("Issues with JSON handling: " + e.getMessage());
        }
    }
}
