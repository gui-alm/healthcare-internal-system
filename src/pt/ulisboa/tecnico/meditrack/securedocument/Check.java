package pt.ulisboa.tecnico.meditrack.securedocument;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.crypto.BadPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

/*
 * Verifies the security of the input file.
 */
public class Check {

    public static void check(byte[] toDecrypt, byte[] key, byte[] IV) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, 
    InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, ShortBufferException, FileNotFoundException, 
    IOException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, IV));
        cipher.updateAAD("meditrack".getBytes());
        cipher.doFinal(toDecrypt);
    }

    public static void checkJSON(JsonObject json, String client) {
        for (Map.Entry<String, JsonElement> jsonEntry : json.entrySet()) {
            try {
                JsonObject section = jsonEntry.getValue().getAsJsonObject(); // Contents + metadata
                JsonArray metadata = section.get("metadata").getAsJsonArray(); // Metadata
                JsonElement test = section.get("contents");
                // If, for some reason, the section is not encrypted aka is in JSON format
                if (test.isJsonObject() || test.isJsonArray()) continue;
                byte[] contents = Common.decodeBase64(test.getAsString()); // Contents

                boolean keysFound = false;
                for (int i = 0; i < metadata.size(); i++) {
                    JsonObject encryptionData = metadata.get(i).getAsJsonObject();
                    String jsonClient = encryptionData.get("client").getAsString();
                    if (!jsonClient.toLowerCase().equals(client.toLowerCase())) continue;

                        // Both key and IV fields are encoded in Base64
                        byte[] encryptedKey = Common.decodeBase64(encryptionData.get("key").getAsString());
                        byte[] encryptedIV = Common.decodeBase64(encryptionData.get("IV").getAsString());
                        
                        byte[] privKey = Common.readPrivKey(client);
                        
                        byte[] symKey = Common.asymDecrypt(encryptedKey, privKey);
                        byte[] IV = Common.asymDecrypt(encryptedIV, privKey);
                        Common.decrypt(contents, symKey, IV);
                        System.out.println("Integrity of field " + jsonEntry.getKey() + " is OK!");
                        keysFound = true;
                        // We already found the corresponding client's key
                        break;
                        
                }
                if (!keysFound) System.err.println("Could not verify integrity of field " + jsonEntry.getKey() + ". The given user has no decrypting permissions.");
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                System.err.println("Could not verify integrity of field " + jsonEntry.getKey() + ". Invalid cipher instance");
            } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
                System.err.println("Could not verify integrity of field " + jsonEntry.getKey() + ". Issue with symmetric decryption: " + e.getMessage());
            } catch (InvalidKeySpecException e) {
                System.err.println("Could not verify integrity of field " + jsonEntry.getKey() + ". Issue with asymmetric decryption: " + e.getMessage());
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                System.err.println("Integrity of field " + jsonEntry.getKey() + " is not OK!");
            } catch (FileNotFoundException e) {
                System.err.println("Could not verify integrity of field " + jsonEntry.getKey() + ". Private key file not found: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Could not verify integrity of field " + jsonEntry.getKey() + ". Issue reading key from file: " + e.getMessage());
            } catch (UnsupportedOperationException | IllegalStateException e) {
                System.err.println("Could not verify integrity of field " + jsonEntry.getKey() + ". Issue with JSON handling: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        
        if (args.length < 2) {
            System.err.println("Arguments missing!");
            System.err.println("Usage: check (input-file) (client)");
            return;
        }

        final String inputFile = args[0];
        final String client = args[1];

        try {
            JsonObject json = Common.readJSON(inputFile);
            checkJSON(json, client);
        } catch (FileNotFoundException e) {
            System.err.printf("Could not verify integrity. %s was not found.%n", inputFile);
        }
    }
}