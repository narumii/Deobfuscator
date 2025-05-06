package qprotect;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class AESStringEncryption {
  public static void main(String[] args) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {
    // Dynamic - IV array
    byte[] ivArray = new byte[16];
    ivArray[9] = -77;
    ivArray[3] = -79;
    ivArray[5] = -14;
    ivArray[4] = 117;
    ivArray[11] = -49;
    ivArray[0] = 41;
    ivArray[13] = -94;
    ivArray[1] = 47;
    ivArray[14] = -126;
    ivArray[12] = -107;
    ivArray[7] = 79;
    ivArray[6] = 25;
    ivArray[2] = -49;
    ivArray[8] = 85;
    ivArray[10] = 99;
    ivArray[15] = -82;

    System.out.println(decryptString("YZ6Kaqx0EzNi7hTkyq4QMykvz7F18hlPVbNjz5Wigq5JNJ1YBy/IzSvAUddCsZBdnUP4IW9D/R0Bd/+f7HEJUg==", "�\u001d\u001f�", ivArray));
  }

  public static String decryptString(String base64EncryptedData, String password, byte[] iv) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
    // Decode the Base64 encoded input string
    byte[] decodedData = Base64.getDecoder().decode(base64EncryptedData);

    // Initialize salt array. This will be overwritten by the first 16 bytes of the decoded data.
    // The initial values here seem to be placeholders or defaults that are immediately replaced.
    byte[] salt = new byte[]{124, 26, -30, -113, 87, 0, -111, -97, -126, 91, -12, 50, 77, 75, 6, -4}; // Dynamic

    // The actual encrypted content is after the first 32 bytes of the decoded data.
    // The first 16 bytes are used as the salt, and bytes 17-32 are skipped/unused.
    byte[] encryptedContent = new byte[decodedData.length - 32];

    // Extract the salt from the first 16 bytes of the decoded data
    System.arraycopy(decodedData, 0, salt, 0, 16);
    // Extract the encrypted content, skipping the first 32 bytes (16 for salt, 16 unused)
    System.arraycopy(decodedData, 32, encryptedContent, 0, decodedData.length - 32);

    // Configure the Password-Based Key Derivation Function (PBKDF2)
    // Uses the provided password, extracted salt, an iteration count of 1278, and a key length of 256 bits.
    PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt, 1278, 256); // Dynamic - iteration count
    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

    // Generate the secret key from the PBEKeySpec
    byte[] derivedKey = secretKeyFactory.generateSecret(pbeKeySpec).getEncoded();

    // Create a SecretKeySpec for AES using the derived key
    SecretKeySpec secretKeySpec = new SecretKeySpec(derivedKey, "AES");

    // Initialize the Cipher for AES decryption in CBC mode with PKCS5Padding
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));

    // Perform the decryption
    byte[] decryptedBytes = cipher.doFinal(encryptedContent);

    // Convert the decrypted bytes to a String using UTF-8 encoding
    return new String(decryptedBytes, "UTF-8");
  }
}
