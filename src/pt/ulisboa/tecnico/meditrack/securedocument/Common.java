package pt.ulisboa.tecnico.meditrack.securedocument;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.BadPaddingException;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.Base64;

public class Common {
    
    public static void writeToFile(byte[] bytes, String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(bytes);
        }
    }

    public static byte[] decodeBase64(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    public static String encodeBase64(byte[] toEncode) {
        return Base64.getEncoder().encodeToString(toEncode);
    }

    public static String convertDoctorToFileName(String name) {
        String[] names = name.split(" ");
        return names[0].replaceAll("\\.", "").toLowerCase() + "_" + names[1].toLowerCase();
    }

    public static byte[] readPubKey(String keyUser) throws FileNotFoundException, IOException {
        String user = keyUser.split(" ").length > 1 ? convertDoctorToFileName(keyUser) : keyUser;
        return Common.readFile(System.getProperty("user.home") + "/meditrack/publicKeys/" + user.toLowerCase() + ".pub");
    }

    public static byte[] readPrivKey(String keyUser) throws FileNotFoundException, IOException {
        String user = keyUser.split(" ").length > 1 ? convertDoctorToFileName(keyUser) : keyUser;
        return Common.readFile(System.getProperty("user.home") + "/meditrack/" + user.toLowerCase() + ".priv");
    }

    public static byte[] readFile(String path) throws FileNotFoundException, IOException {
		FileInputStream fis = new FileInputStream(path);
		byte[] content = new byte[fis.available()];
		fis.read(content);
		fis.close();
		return content;
	}
    
    public static JsonObject readJSON(String file) throws FileNotFoundException {
        FileReader fileReader = new FileReader(file);
        Gson gson = new Gson();
        return gson.fromJson(fileReader, JsonObject.class);
    }

    public static void writeJSON(JsonObject json, String file) throws FileNotFoundException, IOException {
        try (FileWriter fileWriter = new FileWriter(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(json, fileWriter);
        }
    }

    public static Key generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128, new SecureRandom());
        return keyGen.generateKey();
    }

    public static byte[] generateIV() {
        byte[] IV = new byte[16];
        SecureRandom secure = new SecureRandom();
        secure.nextBytes(IV);
        return IV;
    }

    public static PublicKey getPublicKey(byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(key);
        KeyFactory keyFacPub = KeyFactory.getInstance("RSA");
		return keyFacPub.generatePublic(pubSpec);
    }

    public static PrivateKey getPrivateKey(byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    public static byte[] asymDecrypt(byte[] toDecrypt, byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException, 
    NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        PrivateKey privateKey = getPrivateKey(key);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(toDecrypt);
    }

    public static byte[] asymEncrypt(byte[] toEncrypt, byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, 
    InvalidKeyException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException, InvalidKeySpecException {
        PublicKey pubKey = Common.getPublicKey(key);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return cipher.doFinal(toEncrypt);
    }

    public static byte[] decrypt(byte[] toDecrypt, byte[] key, byte[] IV) throws NoSuchAlgorithmException, NoSuchPaddingException,
    InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, IV));
        cipher.updateAAD("meditrack".getBytes());
        byte[] decryptedJson = cipher.doFinal(toDecrypt);
        return decryptedJson;
    }

    public static boolean verifySignature(byte[] data, byte[] signature, byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        Signature sig = Signature.getInstance("SHA256withRSA");
        PublicKey publicKey = getPublicKey(key);
        sig.initVerify(publicKey);
        sig.update(data);
        return sig.verify(signature);
    }

    public static byte[] sign(byte[] toSign, byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException,
    InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        PrivateKey privateKey = getPrivateKey(key);
        signature.initSign(privateKey);
        signature.update(toSign);
        return signature.sign();
    }
}
