package qprotect;

public class StringEncryption {
  public static void main(String[] args) {
    System.out.println(decryptString(
        "퇧\uea6e롩톮\uea71롽퇳\uea60", "\u001an��4�", "w\u000bￒﾌAﾜy\u0005ﾙﾈWﾖ5\u001fﾞﾙ_ﾐ5*ﾖﾐpﾜw\u000fﾚﾘ", 2037571890, -791401, 253081604, 1890996
    ));
  }

  /**
   * Decrypts an encrypted string using multiple XOR operations and stack trace validation
   *
   * @param encryptedText The main encrypted text to decrypt
   * @param xorKey The key used for initial XOR operation
   * @param encryptedData Secondary encrypted data
   * @param salt1 First salt value for XOR
   * @param salt2 Second salt value for XOR
   * @param salt3 Third salt value for XOR
   * @param salt4 Fourth salt value for XOR
   * @return Decrypted string
   */
  public static String decryptString(String encryptedText, String xorKey, String encryptedData,
                                     int salt1, int salt2, int salt3, int salt4) {
    // First decryption phase - XOR with key
    char[] encryptedChars = encryptedData.toCharArray();
    char[] decryptedChars = new char[encryptedChars.length];
    char[] keyChars = xorKey.toCharArray();
    //StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

    // Perform initial XOR decryption
    for (int i = 0; i < encryptedChars.length; i++) {
      decryptedChars[i] = (char)(encryptedChars[i] ^ keyChars[i % keyChars.length]);
    }

    // Calculate hash values for verification
    String decryptedString = new String(decryptedChars);
    int stringHash = decryptedString.hashCode();
    int xorValue = salt2 - salt4 - salt1;

    // Stack trace validation (anti-tampering)
    //stackTrace[2].getClassName().hashCode();
    //stackTrace[2].getMethodName().hashCode();

    // Second decryption phase - Triple XOR pattern
    char[] finalEncrypted = encryptedText.toCharArray();
    char[] finalDecrypted = new char[finalEncrypted.length];

    for (int i = 0; i < finalDecrypted.length; i++) {
      switch (i % 3) {
        case 0:
          finalDecrypted[i] = (char)(xorValue ^ stringHash ^ finalEncrypted[i]);
          break;
        case 1:
          finalDecrypted[i] = (char)(salt4 ^ xorValue ^ finalEncrypted[i]);
          break;
        case 2:
          finalDecrypted[i] = (char)(salt3 ^ finalEncrypted[i]);
      }
    }

    return new String(finalDecrypted);
  }
}
