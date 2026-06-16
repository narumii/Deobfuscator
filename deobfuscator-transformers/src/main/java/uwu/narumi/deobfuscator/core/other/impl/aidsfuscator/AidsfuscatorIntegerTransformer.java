package uwu.narumi.deobfuscator.core.other.impl.aidsfuscator;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.FieldRef;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.helper.MethodHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AidsfuscatorIntegerTransformer extends Transformer {
  @Override
  protected void transform() {
    Collection<ClassWrapper> classes = scopedClasses();
    Map<FieldRef, Integer> classSalts = AidsfuscatorClassSaltTransformer.findSalts(classes);
    Map<MethodRef, Integer> methodSalts = AidsfuscatorClassSaltTransformer.findMethodSalts(classes, classSalts);

    Map<MethodRef, IntDecryptorInfo> decryptors = findDecryptors(classes);
    if (decryptors.isEmpty()) return;

    Map<FieldRef, int[]> pools = new HashMap<>();
    for (IntDecryptorInfo decryptor : decryptors.values()) {
      ClassWrapper owner = findClass(classes, decryptor.methodRef.owner()).orElse(null);
      if (owner == null || pools.containsKey(decryptor.numberField)) continue;
      extractNumberPool(owner, decryptor.numberField, classSalts, methodSalts)
          .ifPresent(pool -> pools.put(decryptor.numberField, pool));
    }

    Set<MethodRef> usedDecryptors = new HashSet<>();
    for (ClassWrapper classWrapper : classes) {
      for (MethodNode methodNode : classWrapper.methods()) {
        MethodRef caller = MethodRef.of(classWrapper.classNode(), methodNode);

        Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames;
        try {
          frames = MethodHelper.analyzeSource(classWrapper.classNode(), methodNode);
        } catch (RuntimeException ignored) {
          continue;
        }

        Map<Integer, Integer> locals = MethodHelper.collectKnownIntLocals(
            methodNode,
            frames,
            classSalts,
            methodSalts.get(caller)
        );

        for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
          if (!(insn instanceof MethodInsnNode call) || call.getOpcode() != INVOKESTATIC) continue;

          MethodRef decryptorRef = MethodRef.of(call);
          IntDecryptorInfo decryptor = decryptors.get(decryptorRef);
          if (decryptor == null) continue;

          int[] pool = pools.get(decryptor.numberField);
          if (pool == null) continue;

          Optional<Integer> decrypted = decrypt(decryptor, pool, call, frames, locals, classSalts);
          if (decrypted.isEmpty()) {
            usedDecryptors.add(decryptorRef);
            continue;
          }

          MethodHelper.removeStackProducers(methodNode, call, frames, Type.getArgumentCount(call.desc));
          methodNode.instructions.set(call, AsmHelper.numberInsn(decrypted.get()));
          markChange();
        }
      }
    }

    removeUnusedDecryptors(classes, decryptors.keySet(), usedDecryptors);
    LOGGER.info("Resolved {} Aidsfuscator integer decryptor calls", getChangesCount());
  }

  private Map<MethodRef, IntDecryptorInfo> findDecryptors(Collection<ClassWrapper> classes) {
    Map<MethodRef, IntDecryptorInfo> decryptors = new HashMap<>();

    for (ClassWrapper classWrapper : classes) {
      for (MethodNode methodNode : classWrapper.methods()) {
        if ((methodNode.access & ACC_STATIC) == 0) continue;
        if (!methodNode.desc.equals("(II)I")) continue;

        extractDecryptor(classWrapper, methodNode).ifPresent(info -> decryptors.put(info.methodRef, info));
      }
    }

    return decryptors;
  }

  private Optional<ClassWrapper> findClass(Collection<ClassWrapper> classes, String name) {
    for (ClassWrapper classWrapper : classes) {
      if (classWrapper.name().equals(name)) return Optional.of(classWrapper);
    }
    return Optional.empty();
  }

  private Optional<IntDecryptorInfo> extractDecryptor(ClassWrapper classWrapper, MethodNode methodNode) {
    FieldRef numberField = null;
    for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
      if (insn instanceof FieldInsnNode field && field.getOpcode() == GETSTATIC && field.desc.equals("[I")) {
        numberField = FieldRef.of(field);
        break;
      }
    }
    if (numberField == null) return Optional.empty();

    XorInfo indexXor = null;
    for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
      if (insn.getOpcode() != IXOR) continue;

      AbstractInsnNode constant = AsmHelper.previousMeaningful(insn);
      AbstractInsnNode load = AsmHelper.previousMeaningful(constant);
      AbstractInsnNode next = AsmHelper.nextMeaningful(insn);
      if (!(load instanceof VarInsnNode loadVar) || loadVar.getOpcode() != ILOAD) continue;
      if (constant == null || !constant.isInteger()) continue;
      if (next == null || next.getOpcode() != IALOAD) continue;

      indexXor = new XorInfo(loadVar.var, constant.asInteger());
      break;
    }
    if (indexXor == null) return Optional.empty();

    List<IntMask> masks = extractMasks(methodNode);
    return Optional.of(new IntDecryptorInfo(
        MethodRef.of(classWrapper.classNode(), methodNode),
        numberField,
        indexXor.constant,
        masks
    ));
  }

  private Optional<int[]> extractNumberPool(
      ClassWrapper classWrapper,
      FieldRef numberField,
      Map<FieldRef, Integer> fieldValues,
      Map<MethodRef, Integer> methodSalts
  ) {
    MethodNode clinit = classWrapper.findClInit().orElse(null);
    if (clinit == null) return Optional.empty();

    Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames;
    try {
      frames = MethodHelper.analyzeSource(classWrapper.classNode(), clinit);
    } catch (RuntimeException ignored) {
      return Optional.empty();
    }

    Map<Integer, Integer> locals = MethodHelper.collectKnownIntLocals(
        clinit,
        frames,
        fieldValues,
        methodSalts.get(MethodRef.of(classWrapper.classNode(), clinit))
    );

    for (AbstractInsnNode insn : clinit.instructions.toArray()) {
      if (!(insn instanceof FieldInsnNode putStatic) || putStatic.getOpcode() != PUTSTATIC) continue;
      if (!FieldRef.of(putStatic).equals(numberField)) continue;

      PoolSeed seed = findPoolSeed(clinit, frames, locals, fieldValues, putStatic).orElse(null);
      if (seed == null) continue;

      byte[] bytes = seed.encoded.getBytes(StandardCharsets.ISO_8859_1);
      if (bytes.length % 4 != 0) continue;

      int[] pool = new int[bytes.length / 4];
      for (int i = 0; i < pool.length; i++) {
        int offset = i * 4;
        int value = ((bytes[offset] & 0xff) << 24)
            | ((bytes[offset + 1] & 0xff) << 16)
            | ((bytes[offset + 2] & 0xff) << 8)
            | (bytes[offset + 3] & 0xff);
        pool[i] = value ^ seed.key;
      }

      return Optional.of(pool);
    }

    return Optional.empty();
  }

  private Optional<PoolSeed> findPoolSeed(
      MethodNode clinit,
      Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames,
      Map<Integer, Integer> locals,
      Map<FieldRef, Integer> fieldValues,
      FieldInsnNode putStatic
  ) {
    AbstractInsnNode encodedInsn = null;
    AbstractInsnNode cursor = putStatic;
    while ((cursor = cursor.getPrevious()) != null) {
      if (cursor instanceof LdcInsnNode ldc
          && ldc.cst instanceof String
          && AsmHelper.nextMeaningful(cursor) instanceof LdcInsnNode charset
          && charset.cst instanceof String
          && charset.cst.equals("ISO-8859-1")) {
        encodedInsn = cursor;
        break;
      }
    }
    if (!(encodedInsn instanceof LdcInsnNode encodedLdc)) return Optional.empty();

    AbstractInsnNode keyStore = encodedInsn;
    while ((keyStore = keyStore.getPrevious()) != null) {
      if (!(keyStore instanceof VarInsnNode store) || store.getOpcode() != ISTORE) continue;

      Frame<OriginalSourceValue> frame = frames.get(store);
      if (frame == null || frame.getStackSize() == 0) continue;

      Optional<Integer> key = MethodHelper.evaluateInt(
          frame.getStack(frame.getStackSize() - 1),
          frames,
          locals,
          fieldValues
      );
      if (key.isPresent()) {
        return Optional.of(new PoolSeed((String) encodedLdc.cst, key.get()));
      }
    }

    return Optional.empty();
  }

  private Optional<Integer> decrypt(
      IntDecryptorInfo decryptor,
      int[] pool,
      MethodInsnNode call,
      Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames,
      Map<Integer, Integer> locals,
      Map<FieldRef, Integer> fieldValues
  ) {
    Frame<OriginalSourceValue> frame = frames.get(call);
    if (frame == null || frame.getStackSize() < 2) return Optional.empty();

    Optional<Integer> idxArg = MethodHelper.evaluateInt(frame.getStack(frame.getStackSize() - 2), frames, locals, fieldValues);
    Optional<Integer> keyArg = MethodHelper.evaluateInt(frame.getStack(frame.getStackSize() - 1), frames, locals, fieldValues);
    if (idxArg.isEmpty() || keyArg.isEmpty()) return Optional.empty();

    int index = idxArg.get() ^ decryptor.indexXor;
    if (index < 0 || index >= pool.length) return Optional.empty();

    int value = pool[index] ^ keyArg.get() ^ idxArg.get();
    for (IntMask mask : decryptor.masks) {
      value = mask.apply(value);
    }

    return Optional.of(value);
  }

  private List<IntMask> extractMasks(MethodNode methodNode) {
    AbstractInsnNode valueLoad = null;
    boolean seenArrayLoad = false;
    for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
      if (insn.getOpcode() == IALOAD) {
        seenArrayLoad = true;
        continue;
      }
      if (seenArrayLoad && insn instanceof VarInsnNode load && load.getOpcode() == ILOAD) {
        valueLoad = load;
        break;
      }
    }
    if (valueLoad == null) return List.of();

    List<IntMask> masks = new ArrayList<>();
    AbstractInsnNode cursor = valueLoad;
    while ((cursor = AsmHelper.nextMeaningful(cursor)) != null && cursor.getOpcode() != IRETURN) {
      int opcode = cursor.getOpcode();
      if (opcode != IADD && opcode != ISUB && opcode != IXOR) continue;

      AbstractInsnNode constant = AsmHelper.previousMeaningful(cursor);
      if (constant == null || !constant.isInteger()) continue;
      masks.add(new IntMask(opcode, constant.asInteger()));
    }

    return masks;
  }

  private void removeUnusedDecryptors(Collection<ClassWrapper> classes, Set<MethodRef> decryptors, Set<MethodRef> stillUsed) {
    Set<MethodRef> referenced = new HashSet<>(stillUsed);
    for (ClassWrapper classWrapper : classes) {
      for (MethodNode methodNode : classWrapper.methods()) {
        for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
          if (insn instanceof MethodInsnNode call) {
            MethodRef ref = MethodRef.of(call);
            if (decryptors.contains(ref)) referenced.add(ref);
          }
        }
      }
    }

    for (ClassWrapper classWrapper : classes) {
      boolean removed = classWrapper.methods().removeIf(method -> {
        MethodRef ref = MethodRef.of(classWrapper.classNode(), method);
        return decryptors.contains(ref) && !referenced.contains(ref);
      });
      if (removed) markChange();
    }
  }

  private record IntDecryptorInfo(MethodRef methodRef, FieldRef numberField, int indexXor, List<IntMask> masks) {
  }

  private record PoolSeed(String encoded, int key) {
  }

  private record XorInfo(int local, int constant) {
  }

  private record IntMask(int opcode, int value) {
    int apply(int input) {
      return switch (opcode) {
        case IADD -> input + value;
        case ISUB -> input - value;
        case IXOR -> input ^ value;
        default -> input;
      };
    }
  }
}
