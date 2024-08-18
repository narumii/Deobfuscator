package uwu.narumi.deobfuscator.api.helper;

import java.util.*;
import java.util.function.Predicate;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;
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

  public static Map<AbstractInsnNode, Frame<SourceValue>> analyzeSource(
      ClassNode classNode, MethodNode methodNode) {
    try {
      Map<AbstractInsnNode, Frame<SourceValue>> frames = new HashMap<>();
      Frame<SourceValue>[] framesArray =
          new Analyzer<>(new SourceInterpreter()).analyze(classNode.name, methodNode);
      for (int i = 0; i < framesArray.length; i++) {
        frames.put(methodNode.instructions.get(i), framesArray[i]);
      }
      return frames;
    } catch (Exception e) {
      return null;
    }
  }

  public static Map<AbstractInsnNode, Frame<OriginalSourceValue>> analyzeOriginalSource(
      ClassNode classNode, MethodNode methodNode
  ) {
    try {
      Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames = new HashMap<>();
      Frame<OriginalSourceValue>[] framesArray =
          new Analyzer<>(new OriginalSourceInterpreter()).analyze(classNode.name, methodNode);
      for (int i = 0; i < framesArray.length; i++) {
        frames.put(methodNode.instructions.get(i), framesArray[i]);
      }
      return frames;
    } catch (Exception e) {
      return null;
    }
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
