package uwu.narumi.deobfuscator.core.other.impl.qprotect;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.FieldRef;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FieldMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FrameMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.StringMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.pool.UniversalNumberPoolTransformer;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Transforms AES encrypted strings in qProtect obfuscated code. Example here: {@link qprotect.AESStringEncryption}
 */
public class qProtectAESStringEncryptionTransformer extends Transformer {
  private static final Match DECRYPT_STRING_MATCH = MethodMatch.invokeStatic().desc("(Ljava/lang/String;Ljava/lang/String;[B)Ljava/lang/String;").capture("decrypt-method")
      .and(FrameMatch.stack(0, FieldMatch.getStatic().capture("iv-array")))
      .and(FrameMatch.stack(1, StringMatch.of().capture("password")))
      .and(FrameMatch.stack(2, StringMatch.of().capture("encrypted-data")));

  private final Set<MethodRef> initIVArrayMethods = new HashSet<>();

  @Override
  protected void transform() throws Exception {
    Map<FieldRef, byte[]> ivArrays = new HashMap<>();
    Map<MethodRef, Integer> iterationCounts = new HashMap<>();

    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      DECRYPT_STRING_MATCH.findAllMatches(MethodContext.of(classWrapper, methodNode)).forEach(matchCtx -> {
        // Decrypt method
        MethodInsnNode decryptMethodInsn = (MethodInsnNode) matchCtx.captures().get("decrypt-method").insn();
        MethodRef decryptMethodRef = MethodRef.of(decryptMethodInsn);
        // IV array field
        FieldInsnNode ivArrayFieldInsn = (FieldInsnNode) matchCtx.captures().get("iv-array").insn();
        FieldRef ivArrayFieldRef = FieldRef.of(ivArrayFieldInsn);
        String password = matchCtx.captures().get("password").insn().asString();
        String encryptedData = matchCtx.captures().get("encrypted-data").insn().asString();

        // Get the IV array from the field
        byte[] ivArray = ivArrays.computeIfAbsent(ivArrayFieldRef, (k) -> {
          return extractIvArray(classWrapper, ivArrayFieldRef);
        });

        // Get iteration count
        int iterationCount = iterationCounts.computeIfAbsent(MethodRef.of(decryptMethodInsn), (k) -> {
          // Find the decrypt method
          MethodNode decryptMethod = classWrapper.findMethod(decryptMethodRef).orElseThrow();
          return extractIterationCount(MethodContext.of(classWrapper, decryptMethod));
        });

        // Decrypt string
        String decryptedString = decryptString(encryptedData, password, ivArray, iterationCount);
        methodNode.instructions.insert(matchCtx.insn(), new LdcInsnNode(decryptedString));
        matchCtx.removeAll();

        markChange();
      });
    }));

    // Cleanup
    ivArrays.keySet().forEach(fieldRef -> context().removeField(fieldRef));
    iterationCounts.keySet().forEach(methodRef -> context().removeMethod(methodRef));
    initIVArrayMethods.forEach(methodRef -> {
      context().removeMethod(methodRef);
      // Remove invocation from <clinit>
      ClassWrapper classWrapper = context().getClassesMap().get(methodRef.owner());
      classWrapper.findClInit().ifPresent(clinit -> {
        for (AbstractInsnNode insn : clinit.instructions.toArray()) {
          if (insn.getOpcode() == INVOKESTATIC && insn instanceof MethodInsnNode methodInsn &&
              methodInsn.name.equals(methodRef.name()) && methodInsn.desc.equals(methodRef.desc()) &&
              methodInsn.owner.equals(classWrapper.name())
          ) {
            // Remove invocation
            clinit.instructions.remove(insn);
          }
        }
      });
    });
  }

  private int extractIterationCount(MethodContext decryptMethod) {
    /*
    sipush 1838 // iteration count
    sipush 256
    invokespecial javax/crypto/spec/PBEKeySpec.<init> ([C[BII)V
     */
    Match iterationCountMatch = MethodMatch.invokeSpecial().owner("javax/crypto/spec/PBEKeySpec").name("<init>").desc("([C[BII)V")
        .and(FrameMatch.stack(0, NumberMatch.of()))
        .and(FrameMatch.stack(1, NumberMatch.of().capture("iteration-count")));

    MatchContext matchCtx = iterationCountMatch.findFirstMatch(decryptMethod);
    if (matchCtx == null) {
      throw new IllegalStateException("Could not find iteration count");
    }

    // Get the iteration count from the match context
    return matchCtx.captures().get("iteration-count").insn().asInteger();
  }

  private byte @Nullable [] extractIvArray(ClassWrapper classWrapper, FieldRef ivArrayFieldRef) {
    Match ivArrayMethodMatch = FieldMatch.putStatic().fieldRef(ivArrayFieldRef)
        .and(FrameMatch.stack(0,
            OpcodeMatch.of(NEWARRAY).and(Match.of(ctx -> ((IntInsnNode) ctx.insn()).operand == T_BYTE))
                .and(FrameMatch.stack(0, NumberMatch.of().capture("size")))));

    for (MethodNode methodNode : classWrapper.methods()) {
      // Find match
      MethodContext methodContext = MethodContext.of(classWrapper, methodNode);
      MatchContext ivArrayMatchCtx = ivArrayMethodMatch.findFirstMatch(methodContext);

      if (ivArrayMatchCtx == null) continue;

      int size = ivArrayMatchCtx.captures().get("size").insn().asInteger();

      Number[] ivArrayObj = UniversalNumberPoolTransformer.getFieldNumberPool(methodContext, size, ivArrayFieldRef);
      byte[] ivArray = new byte[size];
      for (int i = 0; i < size; i++) {
        // Convert Number to byte
        ivArray[i] = ivArrayObj[i].byteValue();
      }

      initIVArrayMethods.add(MethodRef.of(classWrapper.classNode(), methodNode));

      return ivArray;
    }

    // Not found
    return null;
  }

  /**
   * Decrypts a Base64 encoded string using AES encryption with a password-based key derivation function (PBKDF2).
   */
  private String decryptString(String base64EncryptedData, String password, byte[] ivArray, int iterationCount) {
    try {
      // Decode the Base64 encoded input string
      byte[] decodedData = Base64.getDecoder().decode(base64EncryptedData);

      // Initialize salt array. This will be overwritten by the first 16 bytes of the decoded data.
      // The initial values here seem to be placeholders or defaults that are immediately replaced.
      //byte[] salt = new byte[]{124, 26, -30, -113, 87, 0, -111, -97, -126, 91, -12, 50, 77, 75, 6, -4}; // Dynamic
      byte[] salt = new byte[16];

      // The actual encrypted content is after the first 32 bytes of the decoded data.
      // The first 16 bytes are used as the salt, and bytes 17-32 are skipped/unused.
      byte[] encryptedContent = new byte[decodedData.length - 32];

      // Extract the salt from the first 16 bytes of the decoded data
      System.arraycopy(decodedData, 0, salt, 0, 16);
      // Extract the encrypted content, skipping the first 32 bytes (16 for salt, 16 unused)
      System.arraycopy(decodedData, 32, encryptedContent, 0, decodedData.length - 32);

      // Configure the Password-Based Key Derivation Function (PBKDF2)
      // Uses the provided password, extracted salt, an iteration count of 1278, and a key length of 256 bits.
      PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt, iterationCount, 256); // Dynamic - iteration count
      SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

      // Generate the secret key from the PBEKeySpec
      byte[] derivedKey = secretKeyFactory.generateSecret(pbeKeySpec).getEncoded();

      // Create a SecretKeySpec for AES using the derived key
      SecretKeySpec secretKeySpec = new SecretKeySpec(derivedKey, "AES");

      // Initialize the Cipher for AES decryption in CBC mode with PKCS5Padding
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(ivArray));

      // Perform the decryption
      byte[] decryptedBytes = cipher.doFinal(encryptedContent);

      // Convert the decrypted bytes to a String using UTF-8 encoding
      return new String(decryptedBytes, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
