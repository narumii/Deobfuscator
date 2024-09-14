package uwu.narumi.deobfuscator.core.other.impl.zkm;

import dev.xdark.ssvm.execution.VMException;
import dev.xdark.ssvm.invoke.Argument;
import dev.xdark.ssvm.mirror.type.InstanceClass;
import dev.xdark.ssvm.value.ObjectValue;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Decrypts {@code long} numbers that uses a technique called Method Parameter Changes. References:
 * <ul>
 * <li>https://www.zelix.com/klassmaster/featuresLongEncryption.html</li>
 * <li>https://www.zelix.com/klassmaster/featuresMethodParameterChanges.html</li>
 * </ul>
 *
 * <p>Example long decrypter usage in zelix:
 * <pre>
 * {@code
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
 * }
 * </pre>
 */
// TODO: Apparently it seems like the order of decrypting sometimes matters due to https://www.zelix.com/klassmaster/featuresMethodParameterChanges.html and https://www.zelix.com/klassmaster/docs/classInitializationOrderStatement.htm
// TODO: Allow to specify class initialization order manually
public class ZelixLongEncryptionMPCTransformer extends Transformer {

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

  private SandBox sandBox;
  private final List<String> processedClasses = new ArrayList<>(); // class internal names

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    sandBox = new SandBox(context);

    context.classes(scope).forEach(classWrapper -> {
      // Don't process already processed classes
      if (processedClasses.contains(classWrapper.name())) return;

      decryptEncryptedLongs(context, classWrapper);
    });

    // Remove decrypter classes
    sandBox.getUsedCustomClasses().forEach(clazz -> context.getClasses().remove(clazz.getInternalName()));
  }

  /**
   * Decrypts encrypted longs
   */
  private void decryptEncryptedLongs(Context context, ClassWrapper classWrapper) {
    // Find clinit
    if (classWrapper.findClInit().isEmpty()) return;
    MethodNode clinit = classWrapper.findClInit().get();

    // Firstly, process encrypted longs in the super class
    if (classWrapper.classNode().superName != null && !classWrapper.classNode().superName.equals("java/lang/Object")) {
      ClassWrapper superClass = context.getClasses().get(classWrapper.classNode().superName);
      if (superClass != null) {
        decryptEncryptedLongs(context, superClass);
      }
    }

    MethodContext methodContext = MethodContext.framed(classWrapper, clinit);

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
          InstanceClass longDecrypterClass = (InstanceClass) sandBox.getMemoryManager().readClass(longDecrypterInstance);

          //String instanceStringified = sandBox.vm().getOperations().toString(longDecrypterInstance);
          //System.out.println(classWrapper.name()+" -> "+instanceStringified);

          if (isSharedDecrypter(longDecrypterClass)) {
            LOGGER.error("Detected Shared Decrypter! Decrypted values may be wrong. Probably classes weren't loaded in proper order.");
          }

          // Decrypt long value
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
        } catch (VMException ex) {
          sandBox.logVMException(ex);
          throw new RuntimeException(ex);
        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
      }
    }

    processedClasses.add(classWrapper.name());
  }

  private boolean isSharedDecrypter(InstanceClass clazz) {
    String selfDesc = "L"+clazz.getInternalName()+";";
    return clazz.staticFieldArea().list().size() == 2
        && clazz.staticFieldArea().list().get(0).getDesc().equals("Z")
        && clazz.staticFieldArea().list().get(1).getDesc().equals(selfDesc)
        && clazz.virtualFieldArea().list().size() == 4
        && clazz.virtualFieldArea().list().get(0).getDesc().equals("Ljava/util/concurrent/ConcurrentHashMap;")
        //&& clazz.virtualFieldArea().list().get(1).getDesc().equals("TODO")
        && clazz.virtualFieldArea().list().get(2).getDesc().equals("[I")
        && clazz.virtualFieldArea().list().get(3).getDesc().equals("J");
  }
}
