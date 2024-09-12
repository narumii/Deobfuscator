package uwu.narumi.deobfuscator.core.other.impl.zkm;

import dev.xdark.ssvm.invoke.Argument;
import dev.xdark.ssvm.mirror.type.InstanceClass;
import dev.xdark.ssvm.value.ObjectValue;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.InstructionContext;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.FieldMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.LongMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.execution.SandBox;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

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
// TODO: Apparently it seems like the order of decrypting sometimes matters due to https://www.zelix.com/klassmaster/featuresMethodParameterChanges.html and https://www.zelix.com/klassmaster/docs/classInitializationOrderStatement.html
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

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    SandBox sandBox = new SandBox(context);

    context.classes(scope).forEach(classWrapper -> classWrapper.findClInit().ifPresent(clinit -> {
      MethodContext methodContext = MethodContext.compute(classWrapper, clinit);

      for (AbstractInsnNode insn : clinit.instructions) {
        InstructionContext insnContext = methodContext.newInsnContext(insn);
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

          ClassWrapper longDecrypterCreatorClass = context.getClasses().get(createDecrypterInsn.owner);

          try {
            // Create decrypter
            InstanceClass clazz = sandBox.getHelper().loadClass(longDecrypterCreatorClass.canonicalName());
            ObjectValue longDecrypterInstance = sandBox.getInvocationUtil().invokeReference(
                clazz.getMethod(createDecrypterInsn.name, createDecrypterInsn.desc),
                Argument.int64(key1), // Key 1
                Argument.int64(key2), // Key 2
                Argument.reference(sandBox.getMemoryManager().nullValue()) // Lookup class
            );

            // Decrypt long value
            InstanceClass longDecrypterClass = (InstanceClass) sandBox.getMemoryManager().readClass(longDecrypterInstance);
            long value = sandBox.getInvocationUtil().invokeLong(
                longDecrypterClass.getMethod(decryptInsn.name, decryptInsn.desc),
                Argument.reference(longDecrypterInstance),
                Argument.int64(decryptKey)
            );

            // Remove all instructions that creates decrypter
            decryptContext.removeAll();

            // Set field to decrypted long value!
            insnContext.methodNode().instructions.insertBefore(insnContext.insn(), AsmHelper.getNumber(value));
            markChange();
          } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }));

    // Remove decrypter classes
    sandBox.getUsedCustomClasses().forEach(clazz -> context.getClasses().remove(clazz.getInternalName()));
  }
}
