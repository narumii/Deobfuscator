package uwu.narumi.deobfuscator.core.other.impl.qprotect;

import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FrameMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.StringMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Transforms encrypted strings in qProtect obfuscated code. Example here: {@link qprotect.StringEncryption}
 */
public class qProtectStringTransformer extends Transformer {
  private static final Match DECRYPT_STRING_MATCH = MethodMatch.invokeStatic().desc("(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IIII)Ljava/lang/String;")
      .and(FrameMatch.stack(0, NumberMatch.numInteger().capture("salt4")))
      .and(FrameMatch.stack(1, NumberMatch.numInteger().capture("salt3")))
      .and(FrameMatch.stack(2, NumberMatch.numInteger().capture("salt2")))
      .and(FrameMatch.stack(3, NumberMatch.numInteger().capture("salt1")))
      .and(FrameMatch.stack(4, StringMatch.of().capture("encryptedData")))
      .and(FrameMatch.stack(5, StringMatch.of().capture("xorKey")))
      .and(FrameMatch.stack(6, StringMatch.of().capture("encryptedText")));

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> {
      AtomicReference<MethodRef> decryptionMethodRef = new AtomicReference<>();

      classWrapper.methods().forEach(methodNode -> {
        DECRYPT_STRING_MATCH.findAllMatches(MethodContext.of(classWrapper, methodNode)).forEach(matchContext -> {
          String encryptedText = matchContext.captures().get("encryptedText").insn().asString();
          String xorKey = matchContext.captures().get("xorKey").insn().asString();
          String encryptedData = matchContext.captures().get("encryptedData").insn().asString();
          int salt1 = matchContext.captures().get("salt1").insn().asInteger();
          int salt2 = matchContext.captures().get("salt2").insn().asInteger();
          int salt3 = matchContext.captures().get("salt3").insn().asInteger();
          int salt4 = matchContext.captures().get("salt4").insn().asInteger();

          MethodInsnNode decryptMethodInsn = (MethodInsnNode) matchContext.insn();
          decryptionMethodRef.set(MethodRef.of(decryptMethodInsn));

          // Decrypt string
          String decryptedString = decryptString(encryptedText, xorKey, encryptedData, salt1, salt2, salt3, salt4);
          methodNode.instructions.insert(matchContext.insn(), new LdcInsnNode(decryptedString));
          matchContext.removeAll();

          markChange();
        });
      });

      // Remove decryption method
      if (decryptionMethodRef.get() != null) {
        classWrapper.methods().removeIf(methodNode -> methodNode.name.equals(decryptionMethodRef.get().name()) && methodNode.desc.equals(decryptionMethodRef.get().desc()));
      }
    });
  }

  /**
   * Decrypts an encrypted string using multiple XOR operations
   *
   * @param encryptedText The main encrypted text to decrypt
   * @param xorKey        The key used for initial XOR operation
   * @param encryptedData Secondary encrypted data
   * @param salt1         First salt value for XOR
   * @param salt2         Second salt value for XOR
   * @param salt3         Third salt value for XOR
   * @param salt4         Fourth salt value for XOR
   * @return Decrypted string
   */
  private static String decryptString(String encryptedText, String xorKey, String encryptedData,
                                      int salt1, int salt2, int salt3, int salt4) {
    // First decryption phase - XOR with key
    char[] encryptedChars = encryptedData.toCharArray();
    char[] decryptedChars = new char[encryptedChars.length];
    char[] keyChars = xorKey.toCharArray();

    // Perform initial XOR decryption
    for (int i = 0; i < encryptedChars.length; i++) {
      decryptedChars[i] = (char) (encryptedChars[i] ^ keyChars[i % keyChars.length]);
    }

    // Calculate hash values for verification
    String decryptedString = new String(decryptedChars);
    int stringHash = decryptedString.hashCode();
    int xorValue = salt2 - salt4 - salt1;

    // Second decryption phase - Triple XOR pattern
    char[] finalEncrypted = encryptedText.toCharArray();
    char[] finalDecrypted = new char[finalEncrypted.length];

    for (int i = 0; i < finalDecrypted.length; i++) {
      switch (i % 3) {
        case 0:
          finalDecrypted[i] = (char) (xorValue ^ stringHash ^ finalEncrypted[i]);
          break;
        case 1:
          finalDecrypted[i] = (char) (salt4 ^ xorValue ^ finalEncrypted[i]);
          break;
        case 2:
          finalDecrypted[i] = (char) (salt3 ^ finalEncrypted[i]);
      }
    }

    return new String(finalDecrypted);
  }
}
