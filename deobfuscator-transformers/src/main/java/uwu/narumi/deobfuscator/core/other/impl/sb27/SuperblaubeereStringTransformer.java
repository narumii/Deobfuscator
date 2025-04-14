package uwu.narumi.deobfuscator.core.other.impl.sb27;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FieldMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FrameMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.NumberMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.OpcodeMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.StringMatch;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Transformer that decrypts strings obfuscated by Superblaubeere27.
 */
public class SuperblaubeereStringTransformer extends Transformer {
  /*
  invokevirtual java/lang/String.getBytes (Ljava/nio/charset/Charset;)[B
  invokevirtual java/security/MessageDigest.digest ([B)[B
  ldc "Blowfish"
  invokespecial javax/crypto/spec/SecretKeySpec.<init> ([BLjava/lang/String;)V
  astore v2
  ldc "Blowfish"
  invokestatic javax/crypto/Cipher.getInstance (Ljava/lang/String;)Ljavax/crypto/Cipher;
   */
  private static final Match STRING_DECRYPT_BLOWFISH_MATCH = SequenceMatch.of(
      MethodMatch.invokeVirtual().owner("java/lang/String").name("getBytes").desc("(Ljava/nio/charset/Charset;)[B"),
      MethodMatch.invokeVirtual().owner("java/security/MessageDigest").name("digest").desc("([B)[B"),
      StringMatch.of("Blowfish"),
      MethodMatch.invokeSpecial().owner("javax/crypto/spec/SecretKeySpec").name("<init>").desc("([BLjava/lang/String;)V")
  );

  /*
  invokevirtual java/lang/String.getBytes (Ljava/nio/charset/Charset;)[B
  invokevirtual java/security/MessageDigest.digest ([B)[B
  bipush 8
  invokestatic java/util/Arrays.copyOf ([BI)[B
  ldc "DES"
  invokespecial javax/crypto/spec/SecretKeySpec.<init> ([BLjava/lang/String;)V
  astore v2
  ldc "DES"
  invokestatic javax/crypto/Cipher.getInstance (Ljava/lang/String;)Ljavax/crypto/Cipher;
   */
  private static final Match STRING_DECRYPT_DES_MATCH = SequenceMatch.of(
      MethodMatch.invokeVirtual().owner("java/lang/String").name("getBytes").desc("(Ljava/nio/charset/Charset;)[B"),
      MethodMatch.invokeVirtual().owner("java/security/MessageDigest").name("digest").desc("([B)[B"),
      NumberMatch.of(8),
      MethodMatch.invokeStatic().owner("java/util/Arrays").name("copyOf").desc("([BI)[B"),
      StringMatch.of("DES"),
      MethodMatch.invokeSpecial().owner("javax/crypto/spec/SecretKeySpec").name("<init>").desc("([BLjava/lang/String;)V")
  );

  /*
  new java/lang/String
  dup
  invokestatic java/util/Base64.getDecoder ()Ljava/util/Base64$Decoder;
  aload p0
  getstatic java/nio/charset/StandardCharsets.UTF_8 Ljava/nio/charset/Charset;
  invokevirtual java/lang/String.getBytes (Ljava/nio/charset/Charset;)[B
  invokevirtual java/util/Base64$Decoder.decode ([B)[B
  getstatic java/nio/charset/StandardCharsets.UTF_8 Ljava/nio/charset/Charset;
  invokespecial java/lang/String.<init> ([BLjava/nio/charset/Charset;)V
   */
  private static final Match STRING_DECRYPT_XOR_MATCH = SequenceMatch.of(
      OpcodeMatch.of(NEW),
      OpcodeMatch.of(DUP),
      MethodMatch.invokeStatic().owner("java/util/Base64").name("getDecoder").desc("()Ljava/util/Base64$Decoder;"),
      OpcodeMatch.of(ALOAD),
      FieldMatch.getStatic(),
      MethodMatch.invokeVirtual().owner("java/lang/String").name("getBytes").desc("(Ljava/nio/charset/Charset;)[B"),
      MethodMatch.invokeVirtual().owner("java/util/Base64$Decoder").name("decode").desc("([B)[B"),
      FieldMatch.getStatic(),
      MethodMatch.invokeSpecial().owner("java/lang/String").name("<init>").desc("([BLjava/nio/charset/Charset;)V")
  );

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> {
      Map<MethodRef, Decryptor> decryptMethods = new HashMap<>();

      // Find decrypt methods
      classWrapper.methods().forEach(methodNode -> {
        if (!methodNode.desc.equals("(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;")) return;

        MethodContext methodContext = MethodContext.of(classWrapper, methodNode);
        if (STRING_DECRYPT_BLOWFISH_MATCH.findFirstMatch(methodContext) != null) {
          decryptMethods.put(MethodRef.of(classWrapper.classNode(), methodNode), SuperblaubeereStringTransformer::decryptStringBlowfish);
        } else if (STRING_DECRYPT_DES_MATCH.findFirstMatch(methodContext) != null) {
          decryptMethods.put(MethodRef.of(classWrapper.classNode(), methodNode), SuperblaubeereStringTransformer::decryptStringDES);
        } else if (STRING_DECRYPT_XOR_MATCH.findFirstMatch(methodContext) != null) {
          decryptMethods.put(MethodRef.of(classWrapper.classNode(), methodNode), SuperblaubeereStringTransformer::decryptXOR);
        }
      });

      if (decryptMethods.isEmpty()) {
        // No decrypt method found
        return;
      }

      Match STRING_DECRYPT_MATCH = MethodMatch.invokeStatic().and(Match.of(ctx -> decryptMethods.containsKey(MethodRef.of((MethodInsnNode)ctx.insn()))))
          .and(FrameMatch.stack(0, StringMatch.of().capture("key")))
          .and(FrameMatch.stack(1, StringMatch.of().capture("encryptedString")));

      // Decrypt all strings
      classWrapper.methods().forEach(methodNode -> STRING_DECRYPT_MATCH.findAllMatches(MethodContext.of(classWrapper, methodNode)).forEach(matchCtx -> {
        String encryptedString = matchCtx.captures().get("encryptedString").insn().asString();
        String key = matchCtx.captures().get("key").insn().asString();
        MethodRef decryptMethodRef = MethodRef.of((MethodInsnNode) matchCtx.insn());

        //System.out.println("Found encrypted string: " + encryptedString);
        //System.out.println("Key: " + key);

        Decryptor decryptor = decryptMethods.get(decryptMethodRef);

        String decryptedString = decryptor.decrypt(encryptedString, key);
        //System.out.println(decryptedString);

        methodNode.instructions.set(matchCtx.insn(), new LdcInsnNode(decryptedString));
        markChange();

        // Cleanup
        Set<AbstractInsnNode> toRemove = new HashSet<>(matchCtx.collectedInsns());
        toRemove.remove(matchCtx.insn()); // Remove self as it is already replaced
        toRemove.forEach(methodNode.instructions::remove);
      }));

      // Remove decrypt methods
      classWrapper.methods().removeIf(methodNode -> decryptMethods.containsKey(MethodRef.of(classWrapper.classNode(), methodNode)));
    });
  }

  private static String decryptStringBlowfish(String encryptedString, String key) {
    try {
      SecretKeySpec secretKeySpec = new SecretKeySpec(MessageDigest.getInstance("MD5").digest(key.getBytes(StandardCharsets.UTF_8)), "Blowfish");
      Cipher cipher = Cipher.getInstance("Blowfish");
      cipher.init(2, secretKeySpec);
      return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedString.getBytes(StandardCharsets.UTF_8))), StandardCharsets.UTF_8);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
             InvalidKeyException e) {
      throw new RuntimeException(e);
    }
  }

  private static String decryptStringDES(String encryptedString, String key) {
    try {
      SecretKeySpec secretKeySpec = new SecretKeySpec(Arrays.copyOf(MessageDigest.getInstance("MD5").digest(key.getBytes(StandardCharsets.UTF_8)), 8), "DES");
      Cipher cipher = Cipher.getInstance("DES");
      cipher.init(2, secretKeySpec);
      return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedString.getBytes(StandardCharsets.UTF_8))), StandardCharsets.UTF_8);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private static String decryptXOR(String encryptedString, String key) {
    encryptedString = new String(Base64.getDecoder().decode(encryptedString.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    StringBuilder builder = new StringBuilder();
    char[] keyChars = key.toCharArray();
    int keyIndex = 0;

    for (char c : encryptedString.toCharArray()) {
      builder.append((char)(c ^ keyChars[keyIndex % keyChars.length]));
      keyIndex++;
    }

    return String.valueOf(builder);
  }

  @FunctionalInterface
  interface Decryptor {
    String decrypt(String encryptedString, String key);
  }
}
