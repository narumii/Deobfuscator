package uwu.narumi.deobfuscator.core.other.impl.aidsfuscator;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.FieldRef;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.helper.MethodHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class AidsfuscatorStringTransformer extends Transformer {
  @Override
  protected void transform() {
    Collection<ClassWrapper> classes = scopedClasses();
    Map<FieldRef, Integer> classSalts = AidsfuscatorClassSaltTransformer.findSalts(classes);
    Map<MethodRef, Integer> methodSalts = AidsfuscatorClassSaltTransformer.findMethodSalts(classes, classSalts);

    Map<MethodRef, DecryptorInfo> decryptors = findDecryptors(classes);
    if (decryptors.isEmpty()) return;

    Map<FieldRef, String[]> pools = new HashMap<>();
    for (DecryptorInfo decryptor : decryptors.values()) {
      ClassWrapper owner = findClass(classes, decryptor.methodRef().owner()).orElse(null);
      if (owner == null) continue;

      if (!pools.containsKey(decryptor.stringField())) {
        extractStringPool(owner, decryptor.stringField(), classSalts, methodSalts)
            .ifPresent(pool -> pools.put(decryptor.stringField(), pool));
      }
    }

    LOGGER.info("Identified {} Aidsfuscator string decryptors and {} string pools", decryptors.size(), pools.size());

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
          DecryptorInfo decryptor = decryptors.get(decryptorRef);
          if (decryptor == null) continue;

          String[] pool = pools.get(decryptor.stringField());
          if (pool == null) continue;

          Optional<String> decrypted = decrypt(decryptor, pool, classWrapper, methodNode, call, frames, locals, classSalts);
          if (decrypted.isEmpty()) {
            usedDecryptors.add(decryptorRef);
            continue;
          }

          MethodHelper.removeStackProducers(methodNode, call, frames, Type.getArgumentCount(call.desc));
          methodNode.instructions.set(call, new LdcInsnNode(decrypted.get()));
          markChange();
        }
      }
    }

    removeUnusedDecryptors(classes, decryptors.keySet(), usedDecryptors);
    LOGGER.info("Resolved {} Aidsfuscator string decryptor calls", getChangesCount());
  }

  private Map<MethodRef, DecryptorInfo> findDecryptors(Collection<ClassWrapper> classes) {
    Map<MethodRef, DecryptorInfo> decryptors = new HashMap<>();

    for (ClassWrapper classWrapper : classes) {
      for (MethodNode methodNode : classWrapper.methods()) {
        if ((methodNode.access & ACC_STATIC) == 0) continue;
        if (!Type.getReturnType(methodNode.desc).equals(Type.getType(String.class))) continue;

        DecryptorInfo info = null;
        if (methodNode.desc.equals("(II)Ljava/lang/String;")) {
          info = extractDefaultDecryptor(classWrapper, methodNode).orElse(null);
        }
        if (info == null && Type.getArgumentCount(methodNode.desc) == 3) {
          info = extractPolymorphicDecryptor(classWrapper, methodNode).orElse(null);
        }

        if (info != null) {
          decryptors.put(info.methodRef(), info);
        }
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

  private Optional<DecryptorInfo> extractDefaultDecryptor(ClassWrapper classWrapper, MethodNode methodNode) {
    FieldRef stringField = firstGetStatic(methodNode, "[Ljava/lang/String;").orElse(null);
    FieldRef cacheField = firstGetStatic(methodNode, "[Ljava/lang/Object;").orElse(null);
    if (stringField == null || cacheField == null || !hasStackTraceGuard(methodNode)) return Optional.empty();

    XorLocal idx = findIndexedXor(methodNode).orElse(null);
    XorLocal trace = findTraceXor(methodNode).orElse(null);
    if (idx == null || trace == null) return Optional.empty();

    int[] keys = Arrays.stream(methodNode.instructions.toArray())
        .filter(TableSwitchInsnNode.class::isInstance)
        .map(TableSwitchInsnNode.class::cast)
        .filter(tableSwitch -> tableSwitch.labels.size() == 32)
        .map(this::extractTableKeys)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
    if (keys == null) return Optional.empty();

    return Optional.of(new DefaultDecryptorInfo(
        MethodRef.of(classWrapper.classNode(), methodNode),
        stringField,
        cacheField,
        idx.constant,
        trace.constant,
        keys
    ));
  }

  private Optional<DecryptorInfo> extractPolymorphicDecryptor(ClassWrapper classWrapper, MethodNode methodNode) {
    FieldRef stringField = firstGetStatic(methodNode, "[Ljava/lang/String;").orElse(null);
    FieldRef cacheField = firstGetStatic(methodNode, "[Ljava/lang/Object;").orElse(null);
    if (stringField == null || cacheField == null || !hasStackTraceGuard(methodNode)) return Optional.empty();

    XorLocal idx = findIndexedXor(methodNode).orElse(null);
    XorLocal trace = findTraceXor(methodNode).orElse(null);
    if (idx == null || trace == null) return Optional.empty();

    List<Integer> argSlots = MethodHelper.argumentSlots(methodNode.access, methodNode.desc);
    int indexArg = argSlots.indexOf(idx.local);
    if (indexArg == -1) return Optional.empty();

    AbstractInsnNode cast = Arrays.stream(methodNode.instructions.toArray())
        .filter(insn -> insn.getOpcode() == I2C)
        .findFirst()
        .orElse(null);
    if (cast == null) return Optional.empty();

    AbstractInsnNode finalXor = AsmHelper.previousMeaningful(cast);
    AbstractInsnNode shift = AsmHelper.previousMeaningful(finalXor);
    AbstractInsnNode shiftBy = AsmHelper.previousMeaningful(shift);
    AbstractInsnNode key2Load = AsmHelper.previousMeaningful(shiftBy);
    AbstractInsnNode key1Xor = AsmHelper.previousMeaningful(key2Load);
    AbstractInsnNode key1Load = AsmHelper.previousMeaningful(key1Xor);
    AbstractInsnNode hashXor = AsmHelper.previousMeaningful(key1Load);
    AbstractInsnNode hashLoad = AsmHelper.previousMeaningful(hashXor);

    if (finalXor == null || finalXor.getOpcode() != IXOR || shift == null || shift.getOpcode() != ISHR) return Optional.empty();
    if (shiftBy == null || !shiftBy.isInteger() || shiftBy.asInteger() != 16) return Optional.empty();
    if (!(key2Load instanceof VarInsnNode key2Var) || key2Var.getOpcode() != ILOAD) return Optional.empty();
    if (key1Xor == null || key1Xor.getOpcode() != IXOR) return Optional.empty();
    if (!(key1Load instanceof VarInsnNode key1Var) || key1Var.getOpcode() != ILOAD) return Optional.empty();
    if (hashXor == null || hashXor.getOpcode() != IXOR) return Optional.empty();
    if (!(hashLoad instanceof VarInsnNode hashVar) || hashVar.getOpcode() != ILOAD || hashVar.var != trace.local) return Optional.empty();

    int key1Arg = argSlots.indexOf(key1Var.var);
    int key2Arg = argSlots.indexOf(key2Var.var);
    if (key1Arg == -1 || key2Arg == -1) return Optional.empty();

    List<IntMask> masks = extractPolymorphicMasks(methodNode, hashLoad);
    if (masks.isEmpty()) return Optional.empty();

    return Optional.of(new PolymorphicDecryptorInfo(
        MethodRef.of(classWrapper.classNode(), methodNode),
        stringField,
        cacheField,
        idx.constant,
        trace.constant,
        indexArg,
        key1Arg,
        key2Arg,
        masks
    ));
  }

  private Optional<String[]> extractStringPool(
      ClassWrapper classWrapper,
      FieldRef stringField,
      Map<FieldRef, Integer> fieldValues,
      Map<MethodRef, Integer> methodSalts
  ) {
    Optional<String[]> simulated = simulateStringPoolInitializer(classWrapper, stringField, fieldValues);
    if (simulated.isPresent()) return simulated;

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
      if (!FieldRef.of(putStatic).equals(stringField)) continue;

      PoolInitializer initializer = findPoolInitializer(clinit, frames, locals, fieldValues, putStatic).orElse(null);
      if (initializer == null) continue;

      if (initializer.xorKeys == null) {
        String[] decoded = splitByLengths(initializer.data, initializer.lengths, initializer.key, false);
        if (decoded != null) return Optional.of(decoded);
        continue;
      }

      String[] decoded = decodeXorInitializer(initializer, false);
      if (decoded != null) return Optional.of(decoded);

      decoded = decodeXorInitializer(initializer, true);
      if (decoded != null) return Optional.of(decoded);
    }

    return Optional.empty();
  }

  private Optional<String[]> simulateStringPoolInitializer(
      ClassWrapper classWrapper,
      FieldRef stringField,
      Map<FieldRef, Integer> fieldValues
  ) {
    MethodNode clinit = classWrapper.findClInit().orElse(null);
    if (clinit == null) return Optional.empty();

    return new StringPoolClinitInterpreter(classWrapper, clinit, stringField, fieldValues).run();
  }

  private Optional<PoolInitializer> findPoolInitializer(
      MethodNode clinit,
      Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames,
      Map<Integer, Integer> locals,
      Map<FieldRef, Integer> fieldValues,
      FieldInsnNode putStatic
  ) {
    AbstractInsnNode lenStringInsn = null;
    AbstractInsnNode cursor = putStatic;
    while ((cursor = cursor.getPrevious()) != null) {
      if (cursor instanceof LdcInsnNode ldc
          && ldc.cst instanceof String
          && AsmHelper.nextMeaningful(cursor) instanceof MethodInsnNode call
          && call.owner.equals("java/lang/String")
          && call.name.equals("toCharArray")
          && call.desc.equals("()[C")) {
        lenStringInsn = cursor;
        break;
      }
    }
    if (!(lenStringInsn instanceof LdcInsnNode lenLdc)) return Optional.empty();

    AbstractInsnNode dataStringInsn = lenStringInsn;
    while ((dataStringInsn = dataStringInsn.getPrevious()) != null) {
      if (dataStringInsn instanceof LdcInsnNode ldc && ldc.cst instanceof String) break;
    }
    if (!(dataStringInsn instanceof LdcInsnNode dataLdc)) return Optional.empty();

    Integer key = null;
    AbstractInsnNode keyStore = dataStringInsn;
    while ((keyStore = keyStore.getPrevious()) != null) {
      if (!(keyStore instanceof VarInsnNode store) || store.getOpcode() != ISTORE) continue;

      Frame<OriginalSourceValue> frame = frames.get(store);
      if (frame == null || frame.getStackSize() == 0) continue;

      Optional<Integer> evaluated = MethodHelper.evaluateInt(
          frame.getStack(frame.getStackSize() - 1),
          frames,
          locals,
          fieldValues
      );
      if (evaluated.isPresent()) {
        key = evaluated.get();
        break;
      }
    }
    if (key == null) return Optional.empty();

    int[] xorKeys = null;
    AbstractInsnNode switchCursor = lenStringInsn;
    while ((switchCursor = switchCursor.getNext()) != null && switchCursor != putStatic) {
      if (switchCursor instanceof TableSwitchInsnNode tableSwitch) {
        xorKeys = extractTableKeys(tableSwitch);
        break;
      }
    }

    return Optional.of(new PoolInitializer((String) dataLdc.cst, (String) lenLdc.cst, key, xorKeys));
  }

  private String[] decodeXorInitializer(PoolInitializer initializer, boolean twice) {
    String[] chunks = splitByLengths(initializer.data, initializer.lengths, initializer.key, twice);
    if (chunks == null) return null;

    String[] decoded = new String[chunks.length];
    for (int i = 0; i < chunks.length; i++) {
      decoded[i] = xor(chunks[i], twice ? initializer.key : 0, initializer.xorKeys);
    }
    return decoded;
  }

  private String xor(String text, int key, int[] keys) {
    char[] chars = text.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      chars[i] = (char) (chars[i] ^ keys[i % keys.length] ^ key);
    }
    return new String(chars);
  }

  private String[] splitByLengths(String data, String lengths, int key, boolean rawLengths) {
    int[] decodedLengths = new int[lengths.length()];
    int total = 0;
    for (int i = 0; i < lengths.length(); i++) {
      int len = rawLengths ? lengths.charAt(i) : lengths.charAt(i) ^ key;
      if (len < 0) return null;
      decodedLengths[i] = len;
      total += len;
    }
    if (total != data.length()) return null;

    String[] result = new String[decodedLengths.length];
    int offset = 0;
    for (int i = 0; i < decodedLengths.length; i++) {
      result[i] = data.substring(offset, offset + decodedLengths[i]);
      offset += decodedLengths[i];
    }
    return result;
  }

  private Optional<String> decrypt(
      DecryptorInfo decryptor,
      String[] pool,
      ClassWrapper callerClass,
      MethodNode callerMethod,
      MethodInsnNode call,
      Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames,
      Map<Integer, Integer> locals,
      Map<FieldRef, Integer> fieldValues
  ) {
    Frame<OriginalSourceValue> frame = frames.get(call);
    if (frame == null) return Optional.empty();

    int argumentCount = Type.getArgumentCount(call.desc);
    if (frame.getStackSize() < argumentCount) return Optional.empty();

    int[] args = new int[argumentCount];
    for (int i = 0; i < argumentCount; i++) {
      OriginalSourceValue source = frame.getStack(frame.getStackSize() - argumentCount + i);
      Optional<Integer> value = MethodHelper.evaluateInt(source, frames, locals, fieldValues);
      if (value.isEmpty()) return Optional.empty();
      args[i] = value.get();
    }

    try {
      if (decryptor instanceof DefaultDecryptorInfo info) {
        return Optional.of(decryptDefault(info, pool, callerClass, callerMethod, args));
      }
      if (decryptor instanceof PolymorphicDecryptorInfo info) {
        return Optional.of(decryptPolymorphic(info, pool, callerClass, callerMethod, args));
      }
      return Optional.empty();
    } catch (RuntimeException ignored) {
      return Optional.empty();
    }
  }

  private String decryptDefault(DefaultDecryptorInfo info, String[] pool, ClassWrapper callerClass, MethodNode callerMethod, int[] args) {
    int index = args[0] ^ info.indexXor;
    String encrypted = pool[index];
    int key = args[1] >> 16;
    int traceKey = traceKey(callerClass, callerMethod, info.traceXor);

    char[] chars = encrypted.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      chars[i] = (char) (chars[i] ^ info.keys[i & (info.keys.length - 1)] ^ key ^ traceKey);
    }
    return new String(chars).intern();
  }

  private String decryptPolymorphic(PolymorphicDecryptorInfo info, String[] pool, ClassWrapper callerClass, MethodNode callerMethod, int[] args) {
    int index = args[info.indexArg] ^ info.indexXor;
    String encrypted = pool[index];
    int firstKey = args[info.key1Arg];
    int key = args[info.key2Arg] >> 16;
    int traceKey = traceKey(callerClass, callerMethod, info.traceXor);

    char[] chars = encrypted.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      int value = chars[i];
      for (IntMask mask : info.masks) {
        value = mask.apply(value);
      }
      chars[i] = (char) (value ^ traceKey ^ firstKey ^ key);
    }
    return new String(chars).intern();
  }

  private int traceKey(ClassWrapper callerClass, MethodNode callerMethod, int traceXor) {
    return ((callerClass.canonicalName().hashCode() ^ callerMethod.name.hashCode()) >> 16) ^ traceXor;
  }

  private void removeUnusedDecryptors(Collection<ClassWrapper> classes, Set<MethodRef> decryptors, Set<MethodRef> stillUsed) {
    if (decryptors.isEmpty()) return;

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

  private Optional<FieldRef> firstGetStatic(MethodNode methodNode, String desc) {
    return Arrays.stream(methodNode.instructions.toArray())
        .filter(FieldInsnNode.class::isInstance)
        .map(FieldInsnNode.class::cast)
        .filter(field -> field.getOpcode() == GETSTATIC && field.desc.equals(desc))
        .map(FieldRef::of)
        .findFirst();
  }

  private boolean hasStackTraceGuard(MethodNode methodNode) {
    boolean hasThrowable = false;
    boolean hasClassName = false;
    boolean hasMethodName = false;
    boolean hasIntern = false;

    for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
      if (!(insn instanceof MethodInsnNode call)) continue;
      hasThrowable |= call.owner.equals("java/lang/Throwable") && call.name.equals("getStackTrace");
      hasClassName |= call.owner.equals("java/lang/StackTraceElement") && call.name.equals("getClassName");
      hasMethodName |= call.owner.equals("java/lang/StackTraceElement") && call.name.equals("getMethodName");
      hasIntern |= call.owner.equals("java/lang/String") && call.name.equals("intern");
    }

    return hasThrowable && hasClassName && hasMethodName && hasIntern;
  }

  private Optional<XorLocal> findIndexedXor(MethodNode methodNode) {
    for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
      if (insn.getOpcode() != IXOR) continue;

      AbstractInsnNode constant = AsmHelper.previousMeaningful(insn);
      AbstractInsnNode load = AsmHelper.previousMeaningful(constant);
      AbstractInsnNode store = AsmHelper.nextMeaningful(insn);
      if (!(load instanceof VarInsnNode varInsn) || varInsn.getOpcode() != ILOAD) continue;
      if (constant == null || !constant.isInteger()) continue;
      if (!(store instanceof VarInsnNode storeVar) || storeVar.getOpcode() != ISTORE) continue;

      return Optional.of(new XorLocal(varInsn.var, storeVar.var, constant.asInteger()));
    }
    return Optional.empty();
  }

  private Optional<XorLocal> findTraceXor(MethodNode methodNode) {
    for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
      if (insn.getOpcode() != IXOR) continue;

      AbstractInsnNode constant = AsmHelper.previousMeaningful(insn);
      AbstractInsnNode shift = AsmHelper.previousMeaningful(constant);
      AbstractInsnNode store = AsmHelper.nextMeaningful(insn);
      if (constant == null || !constant.isInteger()) continue;
      if (shift == null || shift.getOpcode() != ISHR) continue;
      if (!(store instanceof VarInsnNode storeVar) || storeVar.getOpcode() != ISTORE) continue;

      return Optional.of(new XorLocal(-1, storeVar.var, constant.asInteger()));
    }
    return Optional.empty();
  }

  private int[] extractTableKeys(TableSwitchInsnNode tableSwitch) {
    if (tableSwitch.labels == null || tableSwitch.labels.isEmpty()) return null;
    int[] keys = new int[tableSwitch.labels.size()];

    for (int i = 0; i < tableSwitch.labels.size(); i++) {
      AbstractInsnNode valueInsn = AsmHelper.nextMeaningful(tableSwitch.labels.get(i));
      if (valueInsn == null || !valueInsn.isInteger()) return null;
      keys[i] = valueInsn.asInteger();
    }

    return keys;
  }

  private List<IntMask> extractPolymorphicMasks(MethodNode methodNode, AbstractInsnNode stopAt) {
    AbstractInsnNode caload = null;
    for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
      if (insn.getOpcode() == CALOAD) {
        caload = insn;
        break;
      }
    }
    if (caload == null) return List.of();

    List<IntMask> masks = new ArrayList<>();
    AbstractInsnNode cursor = caload;
    while ((cursor = AsmHelper.nextMeaningful(cursor)) != null && cursor != stopAt) {
      int opcode = cursor.getOpcode();
      if (opcode != IADD && opcode != ISUB && opcode != IXOR) continue;

      AbstractInsnNode constant = AsmHelper.previousMeaningful(cursor);
      if (constant == null || !constant.isInteger()) continue;
      masks.add(new IntMask(opcode, constant.asInteger()));
    }

    return masks;
  }

  private sealed interface DecryptorInfo permits DefaultDecryptorInfo, PolymorphicDecryptorInfo {
    MethodRef methodRef();
    FieldRef stringField();
    FieldRef cacheField();
    int indexXor();
    int traceXor();
  }

  private record DefaultDecryptorInfo(
      MethodRef methodRef,
      FieldRef stringField,
      FieldRef cacheField,
      int indexXor,
      int traceXor,
      int[] keys
  ) implements DecryptorInfo {
  }

  private record PolymorphicDecryptorInfo(
      MethodRef methodRef,
      FieldRef stringField,
      FieldRef cacheField,
      int indexXor,
      int traceXor,
      int indexArg,
      int key1Arg,
      int key2Arg,
      List<IntMask> masks
  ) implements DecryptorInfo {
  }

  private record PoolInitializer(String data, String lengths, int key, int[] xorKeys) {
  }

  private record XorLocal(int loadedLocal, int local, int constant) {
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

  private static final class StringPoolClinitInterpreter {
    private static final int MAX_STEPS = 100_000;

    private final ClassWrapper owner;
    private final MethodNode method;
    private final FieldRef stringField;
    private final Map<FieldRef, Integer> knownInts;
    private final AbstractInsnNode[] instructions;
    private final Map<LabelNode, Integer> labels = new HashMap<>();
    private final Map<FieldRef, Object> statics = new HashMap<>();
    private final List<Object> stack = new ArrayList<>();
    private final Object[] locals;

    private int pc;

    private StringPoolClinitInterpreter(
        ClassWrapper owner,
        MethodNode method,
        FieldRef stringField,
        Map<FieldRef, Integer> knownInts
    ) {
      this.owner = owner;
      this.method = method;
      this.stringField = stringField;
      this.knownInts = knownInts;
      this.instructions = method.instructions.toArray();
      this.locals = new Object[Math.max(method.maxLocals + 16, 64)];

      for (int i = 0; i < instructions.length; i++) {
        if (instructions[i] instanceof LabelNode label) labels.put(label, i);
      }
      knownInts.forEach(statics::put);
    }

    private Optional<String[]> run() {
      try {
        for (int steps = 0; pc >= 0 && pc < instructions.length && steps < MAX_STEPS; steps++) {
          AbstractInsnNode insn = instructions[pc];
          pc++;

          if (insn instanceof LabelNode || insn.getType() == AbstractInsnNode.LINE || insn.getType() == AbstractInsnNode.FRAME) {
            continue;
          }

          String[] pool = execute(insn);
          if (pool != null) return Optional.of(pool);
        }
      } catch (RuntimeException ignored) {
        return Optional.empty();
      }
      return Optional.empty();
    }

    private String[] execute(AbstractInsnNode insn) {
      if (insn.isInteger()) {
        push(insn.asInteger());
        return null;
      }

      if (insn instanceof LdcInsnNode ldc) {
        push(ldc.cst);
        return null;
      }

      if (insn instanceof VarInsnNode varInsn) {
        executeVarInsn(varInsn);
        return null;
      }

      if (insn instanceof IincInsnNode iincInsn) {
        locals[iincInsn.var] = asInt(locals[iincInsn.var]) + iincInsn.incr;
        return null;
      }

      if (insn instanceof JumpInsnNode jumpInsn) {
        executeJump(jumpInsn);
        return null;
      }

      if (insn instanceof LookupSwitchInsnNode lookupSwitch) {
        int key = popInt();
        int index = lookupSwitch.keys.indexOf(key);
        jumpTo(index == -1 ? lookupSwitch.dflt : lookupSwitch.labels.get(index));
        return null;
      }

      if (insn instanceof TableSwitchInsnNode tableSwitch) {
        int key = popInt();
        if (key < tableSwitch.min || key > tableSwitch.max) {
          jumpTo(tableSwitch.dflt);
        } else {
          jumpTo(tableSwitch.labels.get(key - tableSwitch.min));
        }
        return null;
      }

      if (insn instanceof TypeInsnNode typeInsn) {
        executeTypeInsn(typeInsn);
        return null;
      }

      if (insn instanceof IntInsnNode intInsn) {
        executeIntInsn(intInsn);
        return null;
      }

      if (insn instanceof FieldInsnNode fieldInsn) {
        return executeFieldInsn(fieldInsn);
      }

      if (insn instanceof MethodInsnNode methodInsn) {
        executeMethodInsn(methodInsn);
        return null;
      }

      executeSimpleInsn(insn);
      return null;
    }

    private void executeVarInsn(VarInsnNode insn) {
      switch (insn.getOpcode()) {
        case ILOAD, ALOAD, LLOAD -> push(locals[insn.var]);
        case ISTORE, ASTORE, LSTORE -> locals[insn.var] = pop();
        default -> throw new IllegalStateException();
      }
    }

    private void executeJump(JumpInsnNode insn) {
      boolean jump = switch (insn.getOpcode()) {
        case GOTO -> true;
        case IFEQ -> popInt() == 0;
        case IFNE -> popInt() != 0;
        case IFLT -> popInt() < 0;
        case IFGE -> popInt() >= 0;
        case IFGT -> popInt() > 0;
        case IFLE -> popInt() <= 0;
        case IFNULL -> pop() == null;
        case IFNONNULL -> pop() != null;
        case IF_ICMPEQ -> {
          int right = popInt();
          int left = popInt();
          yield left == right;
        }
        case IF_ICMPNE -> {
          int right = popInt();
          int left = popInt();
          yield left != right;
        }
        case IF_ICMPLT -> {
          int right = popInt();
          int left = popInt();
          yield left < right;
        }
        case IF_ICMPGE -> {
          int right = popInt();
          int left = popInt();
          yield left >= right;
        }
        case IF_ICMPGT -> {
          int right = popInt();
          int left = popInt();
          yield left > right;
        }
        case IF_ICMPLE -> {
          int right = popInt();
          int left = popInt();
          yield left <= right;
        }
        default -> throw new IllegalStateException();
      };

      if (jump) jumpTo(insn.label);
    }

    private void executeTypeInsn(TypeInsnNode insn) {
      switch (insn.getOpcode()) {
        case NEW -> push(new NewObject(insn.desc));
        case ANEWARRAY -> {
          int length = popInt();
          if (insn.desc.equals("java/lang/String")) {
            push(new String[length]);
          } else {
            push(new Object[length]);
          }
        }
        case CHECKCAST -> {}
        default -> throw new IllegalStateException();
      }
    }

    private void executeIntInsn(IntInsnNode insn) {
      if (insn.getOpcode() == NEWARRAY) {
        int length = popInt();
        switch (insn.operand) {
          case T_INT -> push(new int[length]);
          case T_BYTE -> push(new byte[length]);
          case T_CHAR -> push(new char[length]);
          default -> push(new Object[length]);
        }
        return;
      }
      push(insn.operand);
    }

    private String[] executeFieldInsn(FieldInsnNode insn) {
      FieldRef ref = FieldRef.of(insn);
      switch (insn.getOpcode()) {
        case GETSTATIC -> push(statics.get(ref));
        case PUTSTATIC -> {
          Object value = pop();
          if (ref.equals(stringField) && value instanceof String[] strings) {
            return strings;
          }
          statics.put(ref, value);
        }
        default -> throw new IllegalStateException();
      }
      return null;
    }

    private void executeMethodInsn(MethodInsnNode insn) {
      Type[] argumentTypes = Type.getArgumentTypes(insn.desc);
      Object[] args = new Object[argumentTypes.length];
      for (int i = argumentTypes.length - 1; i >= 0; i--) {
        args[i] = pop();
      }
      Object receiver = insn.getOpcode() == INVOKESTATIC ? null : pop();

      Object result = invokeWhitelisted(insn, receiver, args);
      if (Type.getReturnType(insn.desc).getSort() != Type.VOID) {
        push(result);
      }
    }

    private Object invokeWhitelisted(MethodInsnNode insn, Object receiver, Object[] args) {
      if (insn.owner.equals("java/lang/String") && insn.name.equals("<init>") && insn.desc.equals("([C)V")) {
        if (receiver instanceof NewObject object) object.value = new String((char[]) args[0]);
        return null;
      }

      if (insn.owner.equals("java/lang/String")) {
        String str = asString(receiver);
        return switch (insn.name + insn.desc) {
          case "toCharArray()[C" -> str.toCharArray();
          case "substring(II)Ljava/lang/String;" -> str.substring(asInt(args[0]), asInt(args[1]));
          case "intern()Ljava/lang/String;" -> str.intern();
          case "getBytes(Ljava/lang/String;)[B" -> str.getBytes(Charset.forName(asString(args[0])));
          case "hashCode()I" -> str.hashCode();
          default -> throw new IllegalStateException();
        };
      }

      if (insn.owner.equals("java/lang/Math") && insn.name.equals("floorMod") && insn.desc.equals("(II)I")) {
        return Math.floorMod(asInt(args[0]), asInt(args[1]));
      }

      if (insn.owner.equals("java/lang/invoke/MethodHandles") && insn.name.equals("lookup")) {
        return new LookupToken(owner.canonicalName());
      }

      if (insn.owner.equals("java/lang/invoke/MethodHandles$Lookup") && insn.name.equals("lookupClass")) {
        return new ClassToken(receiver instanceof LookupToken token ? token.className : owner.canonicalName());
      }

      if (insn.owner.equals("java/lang/Integer") && insn.name.equals("valueOf")) return args[0];
      if (insn.owner.equals("java/lang/Long") && insn.name.equals("valueOf")) return args[0];
      if (insn.owner.equals("java/lang/Integer") && insn.name.equals("intValue")) return asInt(receiver);

      if (Type.getReturnType(insn.desc).equals(Type.INT_TYPE)) {
        FieldInsnNode nextPut = nextPutStatic();
        if (nextPut != null) {
          Integer known = knownInts.get(FieldRef.of(nextPut));
          if (known != null) return known;
        }
        return 0;
      }

      throw new IllegalStateException();
    }

    private FieldInsnNode nextPutStatic() {
      for (int i = pc; i < instructions.length; i++) {
        AbstractInsnNode next = instructions[i];
        if (next instanceof LabelNode || next.getType() == AbstractInsnNode.LINE || next.getType() == AbstractInsnNode.FRAME) {
          continue;
        }
        return next instanceof FieldInsnNode field && field.getOpcode() == PUTSTATIC ? field : null;
      }
      return null;
    }

    private void executeSimpleInsn(AbstractInsnNode insn) {
      switch (insn.getOpcode()) {
        case NOP -> {}
        case ACONST_NULL -> push(null);
        case POP -> pop();
        case DUP -> {
          Object value = peek();
          push(value);
        }
        case DUP2 -> {
          Object value2 = pop();
          Object value1 = pop();
          push(value1);
          push(value2);
          push(value1);
          push(value2);
        }
        case SWAP -> {
          Object value2 = pop();
          Object value1 = pop();
          push(value2);
          push(value1);
        }
        case IADD -> push(popInt() + popInt());
        case ISUB -> {
          int right = popInt();
          int left = popInt();
          push(left - right);
        }
        case IMUL -> push(popInt() * popInt());
        case IDIV -> {
          int right = popInt();
          int left = popInt();
          push(left / right);
        }
        case IREM -> {
          int right = popInt();
          int left = popInt();
          push(left % right);
        }
        case IXOR -> push(popInt() ^ popInt());
        case IAND -> push(popInt() & popInt());
        case IOR -> push(popInt() | popInt());
        case ISHL -> {
          int right = popInt();
          int left = popInt();
          push(left << right);
        }
        case ISHR -> {
          int right = popInt();
          int left = popInt();
          push(left >> right);
        }
        case IUSHR -> {
          int right = popInt();
          int left = popInt();
          push(left >>> right);
        }
        case INEG -> push(-popInt());
        case I2L -> push((long) popInt());
        case I2C -> push((int) (char) popInt());
        case LXOR -> {
          long right = asLong(pop());
          long left = asLong(pop());
          push(left ^ right);
        }
        case LMUL -> {
          long right = asLong(pop());
          long left = asLong(pop());
          push(left * right);
        }
        case ARRAYLENGTH -> push(Array.getLength(pop()));
        case CALOAD -> {
          int index = popInt();
          char[] array = (char[]) pop();
          push((int) array[index]);
        }
        case BALOAD -> {
          int index = popInt();
          byte[] array = (byte[]) pop();
          push((int) array[index]);
        }
        case IALOAD -> {
          int index = popInt();
          int[] array = (int[]) pop();
          push(array[index]);
        }
        case AALOAD -> {
          int index = popInt();
          Object array = pop();
          push(((Object[]) array)[index]);
        }
        case CASTORE -> {
          int value = popInt();
          int index = popInt();
          char[] array = (char[]) pop();
          array[index] = (char) value;
        }
        case IASTORE -> {
          int value = popInt();
          int index = popInt();
          int[] array = (int[]) pop();
          array[index] = value;
        }
        case AASTORE -> {
          Object value = pop();
          int index = popInt();
          Object[] array = (Object[]) pop();
          array[index] = value;
        }
        case RETURN -> pc = instructions.length;
        default -> throw new IllegalStateException();
      }
    }

    private void jumpTo(LabelNode label) {
      Integer index = labels.get(label);
      if (index == null) throw new IllegalStateException();
      pc = index;
    }

    private Object pop() {
      if (stack.isEmpty()) throw new IllegalStateException();
      return stack.remove(stack.size() - 1);
    }

    private int popInt() {
      return asInt(pop());
    }

    private Object peek() {
      if (stack.isEmpty()) throw new IllegalStateException();
      return stack.get(stack.size() - 1);
    }

    private void push(Object value) {
      stack.add(value);
    }

    private static int asInt(Object value) {
      if (value == null) return 0;
      if (value instanceof Integer integer) return integer;
      if (value instanceof Character character) return character;
      if (value instanceof Byte byteValue) return byteValue;
      if (value instanceof Short shortValue) return shortValue;
      throw new IllegalStateException();
    }

    private static long asLong(Object value) {
      if (value instanceof Long longValue) return longValue;
      if (value instanceof Integer integer) return integer;
      throw new IllegalStateException();
    }

    private static String asString(Object value) {
      if (value instanceof String string) return string;
      if (value instanceof NewObject object && object.value instanceof String string) return string;
      throw new IllegalStateException();
    }

    private static final class NewObject {
      private final String type;
      private Object value;

      private NewObject(String type) {
        this.type = type;
      }

      @Override
      public String toString() {
        return value == null ? type : value.toString();
      }
    }

    private record LookupToken(String className) {
    }

    private record ClassToken(String className) {
    }
  }
}
