package uwu.narumi.deobfuscator.core.other.impl.aidsfuscator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AidsfuscatorReferenceObfuscationTransformer extends Transformer {
  @Override
  protected void transform() {
    Collection<ClassWrapper> classes = scopedClasses();

    List<ReferenceDispatcher> dispatchers = findDispatchers(classes);
    if (dispatchers.isEmpty()) return;

    Map<String, ReferenceDispatcher> dispatchersByBootstrapOwner = new HashMap<>();
    for (ReferenceDispatcher dispatcher : dispatchers) {
      dispatchersByBootstrapOwner.put(dispatcher.owner.name(), dispatcher);
    }

    boolean resolvedAny;
    do {
      resolvedAny = resolvePass(classes, dispatchersByBootstrapOwner);
    } while (resolvedAny);

    removeResolvedDispatchers(classes, dispatchers, dispatchersByBootstrapOwner);
    LOGGER.info("Resolved {} Aidsfuscator invokedynamic references", getChangesCount());
  }

  private boolean resolvePass(Collection<ClassWrapper> classes, Map<String, ReferenceDispatcher> dispatchersByBootstrapOwner) {
    Map<FieldRef, Integer> classSalts = AidsfuscatorClassSaltTransformer.findSalts(classes);
    Map<MethodRef, Integer> methodSalts = AidsfuscatorClassSaltTransformer.findMethodSalts(classes, classSalts);

    boolean resolvedAny = false;
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
          if (!(insn instanceof InvokeDynamicInsnNode indy)) continue;

          ReferenceDispatcher dispatcher = dispatchersByBootstrapOwner.get(indy.bsm.getOwner());
          if (dispatcher == null) continue;

          Optional<AbstractInsnNode> replacement = resolveReference(dispatcher, indy, frames, locals, classSalts);
          if (replacement.isEmpty()) continue;

          MethodHelper.removeStackProducers(methodNode, indy, frames, 2);
          methodNode.instructions.set(indy, replacement.get());
          markChange();
          resolvedAny = true;
        }
      }
    }
    return resolvedAny;
  }

  private void removeResolvedDispatchers(
      Collection<ClassWrapper> classes,
      List<ReferenceDispatcher> dispatchers,
      Map<String, ReferenceDispatcher> dispatchersByBootstrapOwner
  ) {
    Set<String> stillReferenced = new HashSet<>();
    for (ClassWrapper classWrapper : classes) {
      for (MethodNode methodNode : classWrapper.methods()) {
        for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
          if (insn instanceof InvokeDynamicInsnNode indy && dispatchersByBootstrapOwner.containsKey(indy.bsm.getOwner())) {
            stillReferenced.add(indy.bsm.getOwner());
          }
        }
      }
    }

    for (ReferenceDispatcher dispatcher : dispatchers) {
      if (!stillReferenced.contains(dispatcher.owner.name())) {
        context().getClassesMap().remove(dispatcher.owner.name());
        markChange();
      }
    }
  }

  private List<ReferenceDispatcher> findDispatchers(Collection<ClassWrapper> classes) {
    List<ReferenceDispatcher> dispatchers = new ArrayList<>();

    for (ClassWrapper classWrapper : classes) {
      MethodNode bootstrap = classWrapper.findMethod(
          null,
          "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"
      ).orElse(null);
      if (bootstrap == null) continue;

      MethodNode mainInvoker = classWrapper.findMethod(
          null,
          "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;Ljava/lang/String;Ljava/lang/invoke/MethodType;II)Ljava/lang/invoke/MethodHandle;"
      ).orElse(null);
      if (mainInvoker == null) continue;

      FieldRef refField = classWrapper.fields().stream()
          .filter(field -> field.desc.equals("[Ljava/lang/String;"))
          .map(field -> FieldRef.of(classWrapper.classNode(), field))
          .findFirst()
          .orElse(null);
      if (refField == null) continue;

      Integer indexKey = extractIndexKey(mainInvoker).orElse(null);
      if (indexKey == null) continue;

      Map<Integer, ReferenceKind> kinds = extractReferenceKinds(mainInvoker);
      if (kinds.size() < 6) continue;

      String[] references = extractReferences(classWrapper, refField);
      if (references.length == 0) continue;

      dispatchers.add(new ReferenceDispatcher(classWrapper, refField, indexKey, kinds, references));
    }

    return dispatchers;
  }

  private Optional<Integer> extractIndexKey(MethodNode mainInvoker) {
    for (AbstractInsnNode insn : mainInvoker.instructions.toArray()) {
      if (insn.getOpcode() != IXOR) continue;

      AbstractInsnNode constant = AsmHelper.previousMeaningful(insn);
      AbstractInsnNode load = AsmHelper.previousMeaningful(constant);
      AbstractInsnNode next = AsmHelper.nextMeaningful(insn);
      if (!(load instanceof VarInsnNode varInsn) || varInsn.getOpcode() != ILOAD) continue;
      if (constant == null || !constant.isInteger()) continue;
      if (!(next instanceof VarInsnNode store) || store.getOpcode() != ISTORE) continue;

      return Optional.of(constant.asInteger());
    }
    return Optional.empty();
  }

  private Map<Integer, ReferenceKind> extractReferenceKinds(MethodNode mainInvoker) {
    Map<Integer, ReferenceKind> kinds = new HashMap<>();

    LookupSwitchInsnNode lookupSwitch = null;
    for (AbstractInsnNode insn : mainInvoker.instructions.toArray()) {
      if (insn instanceof LookupSwitchInsnNode sw) {
        lookupSwitch = sw;
        break;
      }
    }
    if (lookupSwitch == null) return kinds;

    Map<LabelNode, List<Integer>> keysByLabel = new HashMap<>();
    for (int i = 0; i < lookupSwitch.keys.size(); i++) {
      keysByLabel.computeIfAbsent(lookupSwitch.labels.get(i), ignored -> new ArrayList<>()).add(lookupSwitch.keys.get(i));
    }

    for (Map.Entry<LabelNode, List<Integer>> entry : keysByLabel.entrySet()) {
      LabelScan scan = scanLabel(entry.getKey());
      if (scan.kind != null) {
        for (Integer key : entry.getValue()) {
          kinds.put(key, scan.kind);
        }
        continue;
      }

      if (scan.getterChar == null || scan.fieldBaseKind == null) continue;
      for (Integer key : entry.getValue()) {
        if (key.equals(scan.getterChar)) {
          kinds.put(key, scan.fieldBaseKind == ReferenceKind.INSTANCE_FIELD ? ReferenceKind.GET_FIELD : ReferenceKind.GET_STATIC);
        } else {
          kinds.put(key, scan.fieldBaseKind == ReferenceKind.INSTANCE_FIELD ? ReferenceKind.PUT_FIELD : ReferenceKind.PUT_STATIC);
        }
      }
    }

    return kinds;
  }

  private LabelScan scanLabel(LabelNode label) {
    AbstractInsnNode cursor = label;
    Integer getterChar = null;
    ReferenceKind fieldBaseKind = null;

    int guard = 0;
    while ((cursor = AsmHelper.nextMeaningful(cursor)) != null && guard++ < 80) {
      if (cursor instanceof MethodInsnNode call && call.owner.equals("java/lang/invoke/MethodHandles$Lookup")) {
        switch (call.name) {
          case "findVirtual":
            return new LabelScan(ReferenceKind.INVOKE_VIRTUAL, null, null);
          case "findStatic":
            return new LabelScan(ReferenceKind.INVOKE_STATIC, null, null);
          case "findGetter", "findSetter":
            if (fieldBaseKind == null) fieldBaseKind = ReferenceKind.INSTANCE_FIELD;
            break;
          case "findStaticGetter", "findStaticSetter":
            if (fieldBaseKind == null) fieldBaseKind = ReferenceKind.STATIC_FIELD;
            break;
          default:
            break;
        }
      }

      if (cursor instanceof JumpInsnNode jump && jump.getOpcode() == IF_ICMPEQ) {
        AbstractInsnNode constant = AsmHelper.previousMeaningful(cursor);
        if (constant != null && constant.isInteger() && getterChar == null) getterChar = constant.asInteger();
      }

      if (cursor.getOpcode() == GOTO && fieldBaseKind != null) break;
    }

    return new LabelScan(null, getterChar, fieldBaseKind);
  }

  private String[] extractReferences(ClassWrapper classWrapper, FieldRef refField) {
    Map<Integer, String> values = new HashMap<>();
    int max = -1;

    for (MethodNode methodNode : classWrapper.methods()) {
      if (!loadsRefField(methodNode, refField)) continue;

      for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
        if (insn.getOpcode() != AASTORE) continue;

        AbstractInsnNode value = AsmHelper.previousMeaningful(insn);
        AbstractInsnNode index = AsmHelper.previousMeaningful(value);
        if (!(value instanceof LdcInsnNode ldc) || !(ldc.cst instanceof String text)) continue;
        if (index == null || !index.isInteger()) continue;

        values.put(index.asInteger(), text);
        max = Math.max(max, index.asInteger());
      }
    }

    if (max < 0) return new String[0];
    String[] refs = new String[max + 1];
    for (Map.Entry<Integer, String> entry : values.entrySet()) {
      refs[entry.getKey()] = entry.getValue();
    }
    return refs;
  }

  private boolean loadsRefField(MethodNode methodNode, FieldRef refField) {
    for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
      if (insn instanceof FieldInsnNode field && field.getOpcode() == GETSTATIC && FieldRef.of(field).equals(refField)) {
        return true;
      }
    }
    return false;
  }

  private Optional<AbstractInsnNode> resolveReference(
      ReferenceDispatcher dispatcher,
      InvokeDynamicInsnNode indy,
      Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames,
      Map<Integer, Integer> locals,
      Map<FieldRef, Integer> fieldValues
  ) {
    ReferenceKind kind = dispatcher.kinds.get((int) indy.name.charAt(0));
    if (kind == null) return Optional.empty();

    Frame<OriginalSourceValue> frame = frames.get(indy);
    if (frame == null || frame.getStackSize() < 2) return Optional.empty();

    Optional<Integer> idxArg = MethodHelper.evaluateInt(frame.getStack(frame.getStackSize() - 2), frames, locals, fieldValues);
    Optional<Integer> keyArg = MethodHelper.evaluateInt(frame.getStack(frame.getStackSize() - 1), frames, locals, fieldValues);
    if (idxArg.isEmpty() || keyArg.isEmpty()) return Optional.empty();

    int index = idxArg.get() ^ dispatcher.indexKey;
    if (index < 0 || index >= dispatcher.references.length || dispatcher.references[index] == null) return Optional.empty();

    int decryptKey = keyArg.get() >>> 16;
    String token = xor(dispatcher.references[index], decryptKey, 0);
    String[] parts = token.split(":", 3);
    if (parts.length != 3) return Optional.empty();

    String owner = parts[0].replace('.', '/');
    String name = parts[1];
    String desc = parts[2];

    return switch (kind) {
      case INVOKE_STATIC -> Optional.of(new MethodInsnNode(INVOKESTATIC, owner, name, desc, false));
      case INVOKE_VIRTUAL -> {
        int opcode = isInterface(owner) ? INVOKEINTERFACE : INVOKEVIRTUAL;
        yield Optional.of(new MethodInsnNode(opcode, owner, name, desc, opcode == INVOKEINTERFACE));
      }
      case GET_STATIC -> Optional.of(new FieldInsnNode(GETSTATIC, owner, name, fieldDesc(desc)));
      case PUT_STATIC -> Optional.of(new FieldInsnNode(PUTSTATIC, owner, name, fieldDesc(desc)));
      case GET_FIELD -> Optional.of(new FieldInsnNode(GETFIELD, owner, name, fieldDesc(desc)));
      case PUT_FIELD -> Optional.of(new FieldInsnNode(PUTFIELD, owner, name, fieldDesc(desc)));
      case INSTANCE_FIELD, STATIC_FIELD -> Optional.empty();
    };
  }

  private String xor(String text, int key1, int key2) {
    char[] chars = text.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      chars[i] = (char) (chars[i] ^ key1 ^ key2);
    }
    return new String(chars);
  }

  private String fieldDesc(String tokenDesc) {
    return tokenDesc.startsWith("()") ? tokenDesc.substring(2) : tokenDesc;
  }

  private boolean isInterface(String owner) {
    ClassNode info = context().getFullClassProvider().getClassInfo(owner);
    return info != null && (info.access & Opcodes.ACC_INTERFACE) != 0;
  }

  private record ReferenceDispatcher(
      ClassWrapper owner,
      FieldRef refField,
      int indexKey,
      Map<Integer, ReferenceKind> kinds,
      String[] references
  ) {
  }

  private enum ReferenceKind {
    INVOKE_VIRTUAL,
    INVOKE_STATIC,
    GET_FIELD,
    GET_STATIC,
    PUT_FIELD,
    PUT_STATIC,
    INSTANCE_FIELD,
    STATIC_FIELD
  }

  private record LabelScan(ReferenceKind kind, Integer getterChar, ReferenceKind fieldBaseKind) {
  }
}
