package uwu.narumi.deobfuscator.api.helper;

import java.util.*;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.JumpPredictingAnalyzer;
import org.objectweb.asm.tree.analysis.OriginalSourceInterpreter;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.context.Context;

/**
 * Used:
 * https://github.com/ItzSomebody/radon/blob/master/src/main/java/me/itzsomebody/radon/utils/ASMUtils.java
 */
public class AsmHelper implements Opcodes {

  public static AbstractInsnNode getNumber(int number) {
    if (number >= -1 && number <= 5) {
      return new InsnNode(number + 3);
    } else if (number >= -128 && number <= 127) {
      return new IntInsnNode(BIPUSH, number);
    } else if (number >= -32768 && number <= 32767) {
      return new IntInsnNode(SIPUSH, number);
    } else {
      return new LdcInsnNode(number);
    }
  }

  public static AbstractInsnNode getNumber(long number) {
    if (number >= 0 && number <= 1) {
      return new InsnNode((int) (number + 9));
    } else {
      return new LdcInsnNode(number);
    }
  }

  public static AbstractInsnNode getNumber(float number) {
    if (number == 0 || number == 1 || number == 2) {
      return new InsnNode((int) (number + 11));
    } else {
      return new LdcInsnNode(number);
    }
  }

  public static AbstractInsnNode getNumber(double number) {
    if (number == 0 || number == 1) {
      return new InsnNode((int) (number + 14));
    } else {
      return new LdcInsnNode(number);
    }
  }

  public static AbstractInsnNode getNumber(Number number) {
    if (number instanceof Integer || number instanceof Byte || number instanceof Short) {
      return getNumber(number.intValue());
    } else if (number instanceof Long) {
      return getNumber(number.longValue());
    } else if (number instanceof Float) {
      return getNumber(number.floatValue());
    } else if (number instanceof Double) {
      return getNumber(number.doubleValue());
    }

    throw new IllegalArgumentException();
  }

  public static void visitNumber(MethodVisitor methodVisitor, int number) {
    if (number >= -1 && number <= 5) {
      methodVisitor.visitInsn(number + 3);
    } else if (number >= -128 && number <= 127) {
      methodVisitor.visitIntInsn(BIPUSH, number);
    } else if (number >= -32768 && number <= 32767) {
      methodVisitor.visitIntInsn(SIPUSH, number);
    } else {
      methodVisitor.visitLdcInsn(number);
    }
  }

  public static void visitNumber(MethodVisitor methodVisitor, long number) {
    if (number >= 0 && number <= 1) {
      methodVisitor.visitInsn((int) (number + 9));
    } else {
      methodVisitor.visitLdcInsn(number);
    }
  }

  public static boolean isAccess(int access, int opcode) {
    return (access & opcode) != 0;
  }

  public static Optional<MethodNode> findMethod(
      ClassNode classNode, MethodInsnNode methodInsnNode) {
    return classNode == null || classNode.methods == null
        ? Optional.empty()
        : classNode.methods.stream()
            .filter(methodNode -> methodNode.name.equals(methodInsnNode.name))
            .filter(methodNode -> methodNode.desc.equals(methodInsnNode.desc))
            .findFirst();
  }

  public static Optional<MethodNode> findMethod(
      ClassNode classNode, Predicate<MethodNode> predicate) {
    return classNode.methods == null
        ? Optional.empty()
        : classNode.methods.stream().filter(predicate).findFirst();
  }

  public static Optional<FieldNode> findField(ClassNode classNode, Predicate<FieldNode> predicate) {
    return classNode.methods == null
        ? Optional.empty()
        : classNode.fields.stream().filter(predicate).findFirst();
  }

  public static Optional<MethodNode> findClInit(ClassNode classNode) {
    return findMethod(classNode, methodNode -> methodNode.name.equals("<clinit>"));
  }

  public static List<AbstractInsnNode> getInstructionsBetween(
      AbstractInsnNode start, AbstractInsnNode end) {
    return getInstructionsBetween(start, end, true, true);
  }

  public static List<AbstractInsnNode> getInstructionsBetween(
      AbstractInsnNode start, AbstractInsnNode end, boolean includeStart, boolean includeEnd) {
    List<AbstractInsnNode> instructions = new ArrayList<>();

    if (includeStart) instructions.add(start);

    while ((start = start.getNext()) != null && start != end) {
      instructions.add(start);
    }

    if (includeEnd) instructions.add(end);

    return instructions;
  }

  /**
   * Analyzes the stack frames of the method
   *
   * @param classNode The owner class
   * @param methodNode Method
   * @return A map which corresponds to: instruction -> its own stack frame
   */
  @NotNull
  @Unmodifiable
  public static Map<AbstractInsnNode, Frame<OriginalSourceValue>> analyzeSource(
      ClassNode classNode, MethodNode methodNode
  ) {
    Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames = new HashMap<>();
    Frame<OriginalSourceValue>[] framesArray;
    try {
      framesArray = new JumpPredictingAnalyzer(new OriginalSourceInterpreter()).analyze(classNode.name, methodNode);
    } catch (AnalyzerException e) {
      throw new RuntimeException(e);
    }
    for (int i = 0; i < framesArray.length; i++) {
      frames.put(methodNode.instructions.get(i), framesArray[i]);
    }
    return Collections.unmodifiableMap(frames);
  }

  /**
   * Removes values from the stack. You can only remove stack values that are one way produced.
   *
   * @param count How many values to remove from top
   */
  public static void removeValuesFromStack(MethodNode methodNode, Frame<OriginalSourceValue> frame, int count) {
    for (int i = 0; i < count; i++) {
      int stackValueIdx = frame.getStackSize() - (i + 1);

      OriginalSourceValue sourceValue = frame.getStack(stackValueIdx);
      // Remove
      methodNode.instructions.remove(sourceValue.getProducer());
    }
  }

  /**
   * Convert constant value to instruction that represents this constant
   *
   * @param value A constant value
   * @return An instruction that represents this constant
   */
  public static AbstractInsnNode toConstantInsn(Object value) {
    if (value == null)
      return new InsnNode(ACONST_NULL);
    if (value instanceof String || value instanceof Type)
      return new LdcInsnNode(value);
    if (value instanceof Number number)
      return getNumber(number);
    if (value instanceof Boolean bool)
      return getNumber(bool ? 1 : 0);
    if (value instanceof Character character)
      return getNumber(character);

    throw new IllegalArgumentException("Not a constant");
  }

  public static InsnList from(AbstractInsnNode... nodes) {
    InsnList insnList = new InsnList();
    for (AbstractInsnNode node : nodes) {
      insnList.add(node);
    }
    return insnList;
  }

  public static InsnList copy(InsnList insnList) {
    InsnList copiedInsnList = new InsnList();
    for (AbstractInsnNode node : insnList.toArray()) {
      copiedInsnList.add(node.clone(Map.of()));
    }

    return copiedInsnList;
  }

  public static MethodNode copyMethod(MethodNode methodNode) {
    MethodNode copyMethod =
        new MethodNode(
            methodNode.access,
            methodNode.name,
            methodNode.desc,
            methodNode.signature,
            methodNode.exceptions.toArray(new String[0]));
    methodNode.accept(copyMethod);

    return copyMethod;
  }

  public void removeField(FieldInsnNode fieldInsnNode, Context context) {
    if (!context.getClasses().containsKey(fieldInsnNode.owner)) return;

    context
        .getClasses()
        .get(fieldInsnNode.owner)
        .fields()
        .removeIf(
            fieldNode ->
                fieldNode.name.equals(fieldInsnNode.name)
                    && fieldNode.desc.equals(fieldInsnNode.desc));
  }
}
