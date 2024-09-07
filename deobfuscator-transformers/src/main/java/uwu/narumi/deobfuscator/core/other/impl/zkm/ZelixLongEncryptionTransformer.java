package uwu.narumi.deobfuscator.core.other.impl.zkm;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.InstructionContext;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.impl.FieldMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.impl.LongMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.execution.SandboxClassLoader;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Decrypts {@code long} numbers <a href="https://www.zelix.com/klassmaster/featuresLongEncryption.html">https://www.zelix.com/klassmaster/featuresLongEncryption.html</a>
 *
 * <p>Example long decrypter usage in zelix:
 * <pre>
 *   ldc 5832394289974403481L
 *   ldc -8943439614781261032L
 *   invokestatic java/lang/invoke/MethodHandles.lookup ()Ljava/lang/invoke/MethodHandles$Lookup;
 *   invokevirtual java/lang/invoke/MethodHandles$Lookup.lookupClass ()Ljava/lang/Class;
 *   // Create decrypter
 *   invokestatic me/frep/vulcan/spigot/Vulcan_m.a (JJLjava/lang/Object;)Lme/frep/vulcan/spigot/Vulcan_a;
 *   ldc 19597665297729L
 *   // Decrypt method
 *   invokeinterface me/frep/vulcan/spigot/Vulcan_a.a (J)J
 *   putstatic io/github/repooper/packetevents/PacketEventsPlugin.a J
 * </pre>
 */
public class ZelixLongEncryptionTransformer extends Transformer {

  private static final Match DECRYPT_LONG_MATCHER = FieldMatch.putStatic().desc("J")
      // Decrypt
      .stack(MethodMatch.invokeInterface().desc("(J)J").save("decrypt-method")
          .stack(LongMatch.of().save("decrypt-key")) // Decrypt key
          // Create decrypter
          .stack(MethodMatch.invokeStatic().and(Match.predicate((context -> ((MethodInsnNode) context.insn()).desc.startsWith("(JJLjava/lang/Object;)")))).save("create-decrypter-method")
              .stack(MethodMatch.invokeVirtual().stack(MethodMatch.invokeStatic())) // Class lookup
              .stack(LongMatch.of().save("key-2")) // Key 2
              .stack(LongMatch.of().save("key-1")) // Key 1
          ));

  private SandboxClassLoader sandboxClassLoader;

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).forEach(classWrapper -> classWrapper.findClInit().ifPresent(clinit -> {
      MethodContext methodContext = MethodContext.create(classWrapper, clinit);

      for (AbstractInsnNode insn : clinit.instructions) {
        InstructionContext insnContext = methodContext.createInsnContext(insn);
        if (insnContext.frame() == null) return;

        MatchContext result = DECRYPT_LONG_MATCHER.matchResult(insnContext);
        if (result != null) {
          // Get instructions from storage
          MethodInsnNode createDecrypterInsn = (MethodInsnNode) result.storage().get("create-decrypter-method").insn();
          MatchContext decryptContext = result.storage().get("decrypt-method");
          MethodInsnNode decryptInsn = (MethodInsnNode) decryptContext.insn();

          // Some keys
          long key1 = result.storage().get("key-1").insn().asLong();
          long key2 = result.storage().get("key-2").insn().asLong();
          long decryptKey = result.storage().get("decrypt-key").insn().asLong();

          ClassWrapper longDecrypterClass = context.getClasses().get(createDecrypterInsn.owner);
          if (this.sandboxClassLoader == null) {
            // Lazily load sandbox classloader
            this.sandboxClassLoader = new SandboxClassLoader(context);
          }

          try {
            Class<?> clazz = Class.forName(longDecrypterClass.canonicalName(), true, sandboxClassLoader);
            // Create decrypter
            Method createDecrypterMethod = clazz.getDeclaredMethod(createDecrypterInsn.name, long.class, long.class, Object.class);
            Object longDecrypter = createDecrypterMethod.invoke(null, key1, key2, null);

            // Decrypt long value
            Method decryptMethod = longDecrypter.getClass().getDeclaredMethod(decryptInsn.name, long.class);
            long value = (long) decryptMethod.invoke(longDecrypter, decryptKey);

            // Remove all instructions that creates decrypter
            decryptContext.removeAll();

            // Set field to decrypted long value!
            insnContext.methodNode().instructions.insertBefore(insnContext.insn(), AsmHelper.getNumber(value));
            markChange();
          } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }));

    // Remove decrypter classes
    this.sandboxClassLoader.getLoadedCustomClasses().forEach(clazzName -> context.getClasses().remove(clazzName));
  }
}
