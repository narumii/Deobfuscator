package uwu.narumi.deobfuscator.core.other.impl.aidsfuscator;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.FieldRef;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.helper.MethodHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AidsfuscatorClassSaltTransformer extends Transformer {
  @Override
  protected void transform() {
    Map<FieldRef, Integer> salts = findSalts(scopedClasses());
    if (salts.isEmpty()) return;

    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
        if (!(insn instanceof FieldInsnNode field) || field.getOpcode() != GETSTATIC || !field.desc.equals("I")) continue;

        Integer value = salts.get(FieldRef.of(field));
        if (value == null) continue;

        methodNode.instructions.set(field, AsmHelper.numberInsn(value));
        markChange();
      }
    }));

    LOGGER.info("Inlined {} Aidsfuscator class salt loads", getChangesCount());
  }

  static Map<FieldRef, Integer> findSalts(Collection<ClassWrapper> classes) {
    Map<Long, Integer> dispatcherValues = collectDispatcherSaltValues(classes);
    if (dispatcherValues.isEmpty()) return Map.of();

    Map<FieldRef, Integer> salts = new HashMap<>();
    for (ClassWrapper classWrapper : classes) {
      MethodNode clinit = classWrapper.findClInit().orElse(null);
      if (clinit == null) continue;

      Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames;
      try {
        frames = MethodHelper.analyzeSource(classWrapper.classNode(), clinit);
      } catch (RuntimeException ignored) {
        continue;
      }

      for (AbstractInsnNode insn : clinit.instructions.toArray()) {
        if (!(insn instanceof FieldInsnNode putStatic) || putStatic.getOpcode() != PUTSTATIC || !putStatic.desc.equals("I")) {
          continue;
        }

        Frame<OriginalSourceValue> frame = frames.get(putStatic);
        if (frame == null || frame.getStackSize() == 0) continue;

        OriginalSourceValue value = frame.getStack(frame.getStackSize() - 1);
        if (!value.isOneWayProduced() || !(value.getProducer() instanceof MethodInsnNode invoke)) continue;
        if (invoke.getOpcode() != INVOKESTATIC || !invoke.desc.equals("(Ljava/lang/Object;I)I")) continue;

        MethodNode retriever = findMethod(classes, MethodRef.of(invoke)).orElse(null);
        if (retriever == null || looksLikeOrderedClassSaltRetriever(retriever)) continue;

        Frame<OriginalSourceValue> invokeFrame = frames.get(invoke);
        if (invokeFrame == null || invokeFrame.getStackSize() < 2) continue;

        Optional<Integer> fakeValue = MethodHelper.evaluateInt(
            invokeFrame.getStack(invokeFrame.getStackSize() - 1),
            frames,
            Map.of(),
            Map.of()
        );
        if (fakeValue.isEmpty()) continue;

        Integer encodedSalt = dispatcherValues.get(fnv1a(classWrapper.canonicalName()));
        if (encodedSalt == null) continue;

        salts.put(FieldRef.of(putStatic), encodedSalt ^ fakeValue.get());
      }
    }

    return Map.copyOf(salts);
  }

  static Map<MethodRef, Integer> findMethodSalts(Collection<ClassWrapper> classes, Map<FieldRef, Integer> fieldValues) {
    Map<String, ClassWrapper> classMap = new HashMap<>();
    for (ClassWrapper classWrapper : classes) {
      classMap.put(classWrapper.name(), classWrapper);
    }

    Map<MethodRef, Integer> salts = new HashMap<>();
    boolean changed;
    int rounds = 0;
    do {
      changed = false;
      rounds++;

      for (ClassWrapper classWrapper : classes) {
        for (MethodNode methodNode : classWrapper.methods()) {
          MethodRef caller = MethodRef.of(classWrapper.classNode(), methodNode);
          Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames;
          try {
            frames = MethodHelper.analyzeSource(classWrapper.classNode(), methodNode);
          } catch (RuntimeException ignored) {
            continue;
          }

          Map<Integer, Integer> locals = MethodHelper.collectKnownIntLocals(methodNode, frames, fieldValues, salts.get(caller));
          for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
            if (!(insn instanceof MethodInsnNode call)) continue;
            if (!MethodHelper.hasTrailingIntArgument(call.desc)) continue;
            if (!classMap.containsKey(call.owner)) continue;

            Frame<OriginalSourceValue> frame = frames.get(call);
            if (frame == null || frame.getStackSize() < Type.getArgumentCount(call.desc)) continue;

            Optional<Integer> salt = MethodHelper.evaluateInt(frame.getStack(frame.getStackSize() - 1), frames, locals, fieldValues);
            if (salt.isEmpty()) continue;

            ClassWrapper owner = classMap.get(call.owner);
            MethodNode target = owner.findMethod(call.name, call.desc).orElse(null);
            if (target == null) continue;

            MethodRef targetRef = MethodRef.of(owner.classNode(), target);
            if (!salts.containsKey(targetRef)) {
              salts.put(targetRef, salt.get());
              changed = true;
            }
          }
        }
      }
    } while (changed && rounds < 8);

    return Map.copyOf(salts);
  }

  private static Map<Long, Integer> collectDispatcherSaltValues(Collection<ClassWrapper> classes) {
    Map<Long, Integer> values = new HashMap<>();

    for (ClassWrapper classWrapper : classes) {
      for (MethodNode methodNode : classWrapper.methods()) {
        AbstractInsnNode[] insns = methodNode.instructions.toArray();
        for (int i = 0; i < insns.length; i++) {
          if (!(insns[i] instanceof MethodInsnNode put) || put.getOpcode() != INVOKEINTERFACE) continue;
          if (!put.owner.equals("java/util/Map") || !put.name.equals("put")) continue;

          Long classHash = null;
          Integer value = null;
          for (int j = Math.max(0, i - 12); j < i; j++) {
            if (insns[j].isLong()) classHash = insns[j].asLong();
            if (insns[j].isInteger()) value = insns[j].asInteger();
          }

          if (classHash != null && value != null) {
            values.put(classHash, value);
          }
        }
      }
    }

    return values;
  }

  private static boolean looksLikeOrderedClassSaltRetriever(MethodNode methodNode) {
    for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
      if (insn instanceof MethodInsnNode call
          && call.owner.equals("java/lang/Throwable")
          && call.name.equals("getStackTrace")) {
        return true;
      }
    }
    return false;
  }

  private static Optional<MethodNode> findMethod(Collection<ClassWrapper> classes, MethodRef ref) {
    for (ClassWrapper classWrapper : classes) {
      ClassNode classNode = classWrapper.classNode();
      if (!classNode.name.equals(ref.owner())) continue;
      return classWrapper.findMethod(ref.name(), ref.desc());
    }
    return Optional.empty();
  }

  private static long fnv1a(String text) {
    long hash = 0xcbf29ce484222325L;
    for (int i = 0; i < text.length(); i++) {
      hash ^= text.charAt(i);
      hash *= 0x100000001b3L;
    }
    return hash;
  }
}
