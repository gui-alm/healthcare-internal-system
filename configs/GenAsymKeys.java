import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.List;


/**
 * Program to read and write asymmetric key files
 */
public class GenAsymKeys {

	public static void main(String[] args) throws Exception {

		// // check args
		// if (args.length < 1) {
		// 	System.err.println("args: (username)");
		// 	return;
		// }

		List<String> users = new ArrayList<>();
		users.add("dr_jordan");
		users.add("dr_anne");
		users.add("dr_steve");
		users.add("trevor");
		users.add("jack");
		users.add("nina");
		users.add("emergency");

		for (String user : users) {
			final String publicKeyPath = String.format("meditrack/publicKeys/%s.pub", user);
			final String privateKeyPath = String.format("meditrack/%s.priv", user);

			System.out.println("Generate and save keys");
			write(publicKeyPath, privateKeyPath, users);
		}
		
		System.out.println("Done.");
	}

	/** Asymmetric cryptography algorithm. */
	private static final String ASYM_ALGO = "RSA";

	/** Asymmetric cryptography key size. */
	private static final int ASYM_KEY_SIZE = 2048;

	public static void write(String publicKeyPath, String privateKeyPath, List<String> users) 
	 throws Exception {

		// generate key pair
		System.out.println("Generating " + ASYM_ALGO + " keys ...");
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ASYM_ALGO);
		System.out.printf("%d bits%n", ASYM_KEY_SIZE);
		keyGen.initialize(ASYM_KEY_SIZE);
		KeyPair key = keyGen.generateKeyPair();

		System.out.println("Public key info:");
		System.out.println("algorithm: " + key.getPublic().getAlgorithm());
		System.out.println("format: " + key.getPublic().getFormat());

		System.out.println("Writing public key to " + publicKeyPath + " ...");
		byte[] pubEncoded = key.getPublic().getEncoded();
		writeFile(publicKeyPath, pubEncoded);
		
		System.out.println("---");

		System.out.println("Private key info:");
		System.out.println("algorithm: " + key.getPrivate().getAlgorithm());
		System.out.println("format: " + key.getPrivate().getFormat());

		System.out.println("Writing private key to '" + privateKeyPath + "' ...");
		byte[] privEncoded = key.getPrivate().getEncoded();
		writeFile(privateKeyPath, privEncoded);
	}

	private static void writeFile(String path, byte[] content) throws FileNotFoundException, IOException {
		File file = new File(path);
		if (!file.getParentFile().exists()){
			file.getParentFile().mkdirs();
		}
		FileOutputStream fos = new FileOutputStream(path);
		fos.write(content);
		fos.close();
	}

}