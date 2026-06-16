package uwu.narumi.deobfuscator.core.other.impl.aidsfuscator;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.FieldRef;
import uwu.narumi.deobfuscator.api.helper.MethodHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AidsfuscatorConstantsFixTransformer extends Transformer {
  @Override
  protected void transform() {
    scopedClasses().forEach(classWrapper -> {
      MethodNode clinit = classWrapper.findClInit().orElse(null);
      if (clinit == null) return;

      Map<FieldRef, Integer> puts = countStaticPuts(classWrapper.name());

      Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames;
      try {
        frames = MethodHelper.analyzeSource(classWrapper.classNode(), clinit);
      } catch (RuntimeException ignored) {
        return;
      }

      for (AbstractInsnNode insn : clinit.instructions.toArray()) {
        if (!(insn instanceof FieldInsnNode putStatic) || putStatic.getOpcode() != PUTSTATIC) continue;
        if (!putStatic.owner.equals(classWrapper.name())) continue;

        FieldRef fieldRef = FieldRef.of(putStatic);
        if (puts.getOrDefault(fieldRef, 0) != 1) continue;

        FieldNode fieldNode = classWrapper.findField(putStatic.name, putStatic.desc).orElse(null);
        if (fieldNode == null || fieldNode.value != null) continue;
        if ((fieldNode.access & ACC_STATIC) == 0 || (fieldNode.access & ACC_FINAL) == 0) continue;

        Object value = constantAssignedAt(putStatic, frames).orElse(null);
        if (!isValidConstantValue(putStatic.desc, value)) continue;

        fieldNode.value = normalizeConstantValue(putStatic.desc, value);
        MethodHelper.removeStackProducers(clinit, putStatic, frames, 1);
        if (clinit.instructions.contains(putStatic)) {
          clinit.instructions.remove(putStatic);
        }
        markChange();
      }
    });

    LOGGER.info("Recovered {} Aidsfuscator moved constant field values", getChangesCount());
  }

  private Map<FieldRef, Integer> countStaticPuts(String owner) {
    Map<FieldRef, Integer> puts = new HashMap<>();
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
        if (insn instanceof FieldInsnNode field && field.getOpcode() == PUTSTATIC && field.owner.equals(owner)) {
          puts.merge(FieldRef.of(field), 1, Integer::sum);
        }
      }
    }));
    return puts;
  }

  private Optional<Object> constantAssignedAt(FieldInsnNode putStatic, Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames) {
    Frame<OriginalSourceValue> frame = frames.get(putStatic);
    if (frame == null || frame.getStackSize() == 0) return Optional.empty();

    OriginalSourceValue source = frame.getStack(frame.getStackSize() - 1);
    if (source.getConstantValue() == null) return Optional.empty();
    return Optional.ofNullable(source.getConstantValue().get());
  }

  private boolean isValidConstantValue(String desc, Object value) {
    if (value == null) return false;

    Type type = Type.getType(desc);
    return switch (type.getSort()) {
      case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> value instanceof Number;
      case Type.LONG -> value instanceof Long;
      case Type.FLOAT -> value instanceof Float;
      case Type.DOUBLE -> value instanceof Double;
      case Type.OBJECT -> desc.equals("Ljava/lang/String;") && value instanceof String;
      default -> false;
    };
  }

  private Object normalizeConstantValue(String desc, Object value) {
    Type type = Type.getType(desc);
    if (type.getSort() == Type.BOOLEAN || type.getSort() == Type.CHAR || type.getSort() == Type.BYTE || type.getSort() == Type.SHORT || type.getSort() == Type.INT) {
      return ((Number) value).intValue();
    }
    return value;
  }
}
