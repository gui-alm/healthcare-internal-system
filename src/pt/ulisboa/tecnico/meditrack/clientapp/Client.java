package pt.ulisboa.tecnico.meditrack.clientapp;

import pt.ulisboa.tecnico.meditrack.securedocument.Common;
import pt.ulisboa.tecnico.meditrack.securedocument.Unprotect;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;

import java.security.KeyStore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Client {
    
    public static void printFunctionalities() {
        System.out.println("These are the available functionalities for this app:");
        System.out.println("General commands:");
        System.out.println("- Show all available commands aka this message. Command: Help");
        System.out.println("- Quit the program. Command: Quit");
        System.out.println("As a doctor:");
        System.out.println("- Read the records from a patient. Command: Read");
        System.out.println("- Register a new consultation. Command: Register");
        System.out.println("As a patient:");
        System.out.println("- Read your own records. Command: Read");
        System.out.println("- Revoke or grant permissions to your records. Command: Change");
        System.out.println("All commands are case-insensitive.");
        System.out.println("Program should be run with flag \"--emergency\" for emergency mode.");
    }

    public static void printJson(JsonObject json) {
        List<String> toRemove = new ArrayList<String>();
        for (Map.Entry<String, JsonElement> jsonEntry : json.entrySet()) {
            JsonObject section = jsonEntry.getValue().getAsJsonObject();
            section.remove("metadata");
            JsonElement contentsTest = section.get("contents");
            if (!contentsTest.isJsonObject() && !contentsTest.isJsonArray()) toRemove.add(jsonEntry.getKey());
        }
        for (int i = 0; i < toRemove.size(); i++) json.remove(toRemove.get(i));
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        gson.toJson(json, System.out);
        System.out.println("");
    }

    public static HttpResponse<String> readAPICall(JsonObject json, HttpClient httpClient) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://192.168.0.186:8081/read"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                            .build();

        System.out.println("Sending request...");

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static void read(String user, String patient, byte[] privKey, HttpClient httpClient) {
        // Make message
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("command", "read");
        JsonObject messageInfo = new JsonObject();
        messageInfo.addProperty("sender", user);
        messageInfo.addProperty("patient", patient);
        requestJson.add("info", messageInfo);
        requestJson.addProperty("uuid", UUID.randomUUID().toString());
        // Sign message
        try {
            byte[] signature = Common.sign(new Gson().toJson(requestJson).getBytes(), privKey);
            String base64Signature = Common.encodeBase64(signature);
            requestJson.addProperty("signature", base64Signature);
            HttpResponse<String> response = readAPICall(requestJson, httpClient);
            String regex = "^ERROR: (.+)$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(response.body());
            if (matcher.find()) System.out.println("An error in the server has occured: " + matcher.group(1));
            else {
                JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
                Unprotect.decryptJSON(responseJson, user);
                printJson(responseJson);
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException | SignatureException e) {
            System.err.println("Error signing message: " + e.getMessage());
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
        } catch (IOException | InterruptedException e) {
            System.err.println("Error communicating with server: " + e.getMessage());
        } catch (NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            System.err.println("Error decrypting JSON returned by server");
        }
    }
    
    public static HttpResponse<String> registerAPICall(JsonObject json, HttpClient httpClient) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://192.168.0.186:8081/register"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                            .build();

        System.out.println("Sending request...");

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static void registerConsultation(String user, String patient, String date, String speciality, String practice, 
    String summary, byte[] privKey, HttpClient httpClient) {
        // Make message
        JsonObject json = new JsonObject();
        json.addProperty("command", "register");
        // Sign message
        try {
            JsonObject messageInfo = new JsonObject();
            messageInfo.addProperty("sender", user);
            messageInfo.addProperty("patient", patient);
            JsonObject consultation = new JsonObject();
            consultation.addProperty("date", date);
            consultation.addProperty("speciality", speciality);
            consultation.addProperty("doctor", user);
            consultation.addProperty("practice", practice);
            consultation.addProperty("summary", summary);
            messageInfo.add("consultation", consultation);
            byte[] consultationSignature = Common.sign(new Gson().toJson(consultation).getBytes(), privKey);
            messageInfo.addProperty("consultationSignature", Common.encodeBase64(consultationSignature));
            json.add("info", messageInfo);

            json.addProperty("uuid", UUID.randomUUID().toString());

            byte[] signature = Common.sign(new Gson().toJson(json).getBytes(), privKey);
            String base64Signature = Common.encodeBase64(signature);
            json.addProperty("signature", base64Signature);
            HttpResponse<String> response = registerAPICall(json, httpClient);
            String regex = "^ERROR: (.+)$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(response.body());
            if (matcher.find()) System.out.println("An error in the server has occured: " + matcher.group(1));
            else System.out.println("Consultation registered successfully.");
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException | SignatureException e) {
            System.err.println("Error signing message: " + e.getMessage());
        } catch (IOException | InterruptedException e) {
            System.err.println("Error communicating with server: " + e.getMessage());
        }
    }

    public static HttpResponse<String> changeAPICall(JsonObject json, HttpClient httpClient) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://192.168.0.186:8081/change"))
                            .header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofString(json.toString()))
                            .build();

        System.out.println("Sending request...");

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static void changePermission(String user, String doctor, String permission, List<String> sections, byte[] privKey, HttpClient httpClient) {
        // Make message
        JsonObject json = new JsonObject();
        json.addProperty("command", "change");
        JsonObject messageInfo = new JsonObject();
        messageInfo.addProperty("sender", user);
        messageInfo.addProperty("doctor", doctor);
        messageInfo.addProperty("action", permission);
        JsonArray sectionArray = new JsonArray();
        for (int i = 0; i < sections.size(); i++) {
            sectionArray.add(sections.get(i));
        }
        messageInfo.add("sections", sectionArray);
        json.add("info", messageInfo);
        json.addProperty("uuid", UUID.randomUUID().toString());
        // Sign message
        try {
            byte[] signature = Common.sign(new Gson().toJson(json).getBytes(), privKey);
            String base64Signature = Common.encodeBase64(signature);
            json.addProperty("signature", base64Signature);
            HttpResponse<String> response = changeAPICall(json, httpClient);
            String regex = "^ERROR: (.+)$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(response.body());
            if (matcher.find()) System.out.println("An error in the server has occured: " + matcher.group(1));
            else System.out.println("Permissions changed successfully.");
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException | SignatureException e) {
            System.err.println("Error signing message: " + e.getMessage());
        } catch (IOException | InterruptedException e) {
            System.err.println("Error communicating with server: " + e.getMessage());
        }
    }

    public static void setupHTTPS() throws FileNotFoundException, KeyStoreException, IOException, 
    NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        String path = "src/pt/ulisboa/tecnico/meditrack/clientapp/keystore.jks";
        String pw = "sysadmin";
        try (FileInputStream fis = new FileInputStream(path)) {
            keyStore.load(fis, pw.toCharArray());
        }
        System.setProperty("javax.net.ssl.trustStore", path);
        System.setProperty("javax.net.ssl.trustStorePassword", pw);    
    }

    public static void main(String[] args) {

        boolean emergency = false;
        String name, role;

        if (args.length > 0) {
            for (String arg : args) {
                if (arg.equals("--emergency")) emergency = true;
            }
        }
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the Meditrack app!");
        if (!emergency) {
            System.out.println("Who are you?");
            name = scanner.nextLine();
            role = name.split(" ").length > 1 ? "doctor" : "patient";
        } else {
            name = "emergency";
            role = "emergency";
        }
        try {
            setupHTTPS();
            HttpClient httpClient = HttpClient.newBuilder()
                                    .build();

            byte[] privKey = Common.readPrivKey(name);

            System.out.println("Hello " + name + "! :). You're a " + role + ".");
            printFunctionalities();

            while (true) {
                System.out.println("Please choose an operation.");
                String command = scanner.nextLine().toLowerCase();
                switch (command) {
                    case "read":
                        if (role != "patient") {
                            System.out.println("Please insert the patient whose data you wish to read");
                            String patient = scanner.nextLine();
                            read(name, patient, privKey, httpClient);
                        } else read(name, name, privKey, httpClient);
                        break;
                    case "register":
                        if (role == "patient") System.out.println("You cannot execute this command");
                        else {
                            System.out.println("Please insert the patient's name");
                            String patient = scanner.nextLine();
                            System.out.println("Please insert the consultation's date");
                            String date = scanner.nextLine();
                            System.out.println("Please insert the consultation's specialty");
                            String specialty = scanner.nextLine();
                            System.out.println("Please insert the practice's name");
                            String practice = scanner.nextLine();
                            System.out.println("Please insert a brief summary of the consultation");
                            String summary = scanner.nextLine();
                            registerConsultation(name, patient, date, specialty, practice, summary, privKey, httpClient);
                        }
                        break;
                    case "change":
                        if (role == "doctor") System.out.println("You cannot execute this command");
                        else {
                            System.out.println("Please insert the person whose permissions you wish to change.");
                            String target = scanner.nextLine();
                            System.out.println("Please insert the permission change (grant/revoke)");
                            String permission = scanner.nextLine().toLowerCase();
                            System.out.println("Please insert all the sections you wish to change. Press enter again when you're done.");
                            List<String> sections = new ArrayList<String>();
                            String line = null;
                            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                                sections.add(line);
                            }
                            changePermission(name, target, permission, sections, privKey, httpClient);
                        }
                        break;
                    case "help":
                        printFunctionalities();
                        break;
                    case "quit":
                        System.out.println("Goodbye! ;)");
                        scanner.close();
                        System.exit(0);
                    default:
                        System.out.println("Unknown command. Try again!");
                        break;
                    }
                }
        } catch (FileNotFoundException e) {
            System.err.println("File was not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Issue with IO: " + e.getMessage());
        } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | KeyManagementException | UnrecoverableKeyException e) {
            System.err.println("Error setting up HTTPS");
        }  
        finally {
            scanner.close();
        }
    }
}