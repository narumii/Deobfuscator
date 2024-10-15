package uwu.narumi.deobfuscator.api.helper;

import java.util.*;
import java.util.function.Predicate;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Value;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.context.Context;

public class AsmHelper implements Opcodes {

  /**
   * Very useful utility that converts number to corresponding ASM instruction.
   *
   * @param number The number
   * @return An ASM instruction that represents this number
   */
  public static AbstractInsnNode numberInsn(int number) {
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

  public static AbstractInsnNode numberInsn(long number) {
    if (number >= 0 && number <= 1) {
      return new InsnNode((int) (number + 9));
    } else {
      return new LdcInsnNode(number);
    }
  }

  public static AbstractInsnNode numberInsn(float number) {
    if (number == 0 || number == 1 || number == 2) {
      return new InsnNode((int) (number + 11));
    } else {
      return new LdcInsnNode(number);
    }
  }

  public static AbstractInsnNode numberInsn(double number) {
    if (number == 0 || number == 1) {
      return new InsnNode((int) (number + 14));
    } else {
      return new LdcInsnNode(number);
    }
  }

  public static AbstractInsnNode numberInsn(Number number) {
    if (number instanceof Integer || number instanceof Byte || number instanceof Short) {
      return numberInsn(number.intValue());
    } else if (number instanceof Long) {
      return numberInsn(number.longValue());
    } else if (number instanceof Float) {
      return numberInsn(number.floatValue());
    } else if (number instanceof Double) {
      return numberInsn(number.doubleValue());
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
      return numberInsn(number);
    if (value instanceof Boolean bool)
      return numberInsn(bool ? 1 : 0);
    if (value instanceof Character character)
      return numberInsn(character);

    throw new IllegalArgumentException("Not a constant");
  }

  public static Type getTypeFromPrimitiveCast(MethodInsnNode insn) {
    if (insn.getOpcode() != INVOKEVIRTUAL) throw new IllegalArgumentException("Instruction is not an INVOKEVIRTUAL");

    if (insn.owner.equals("java/lang/Byte") && insn.name.equals("byteValue")) return Type.BYTE_TYPE;
    if (insn.owner.equals("java/lang/Short") && insn.name.equals("shortValue")) return Type.SHORT_TYPE;
    if (insn.owner.equals("java/lang/Integer") && insn.name.equals("intValue")) return Type.INT_TYPE;
    if (insn.owner.equals("java/lang/Long") && insn.name.equals("longValue")) return Type.LONG_TYPE;
    if (insn.owner.equals("java/lang/Double") && insn.name.equals("doubleValue")) return Type.DOUBLE_TYPE;
    if (insn.owner.equals("java/lang/Float") && insn.name.equals("floatValue")) return Type.FLOAT_TYPE;
    if (insn.owner.equals("java/lang/Boolean") && insn.name.equals("booleanValue")) return Type.BOOLEAN_TYPE;

    throw new IllegalStateException("Unexpected value: " + insn.owner+"."+insn.name+insn.desc);
  }

  public static AbstractInsnNode toPop(Value value) {
    return value.getSize() == 1 ? new InsnNode(POP) : new InsnNode(POP2);
  }

  /**
   * Update method descriptor in the current class, a superclass and interfaces
   *
   * @param context Deobfuscator context
   * @param methodContext Method context
   * @param desc New method descriptor
   */
  public static void updateMethodDescriptor(Context context, MethodContext methodContext, String desc) {
    ClassWrapper classWrapper = methodContext.classWrapper();
    MethodNode methodNode = methodContext.methodNode();

    tryUpdateMethodDescriptor(context, classWrapper, methodNode.name, methodNode.desc, desc);
  }

  /**
   * Tries to update method descriptor in the current class, a superclass and interfaces
   *
   * @param context Deobfuscator context
   * @param classWrapper A class to check
   * @param name Method name
   * @param oldDesc Old method descriptor
   * @param newDesc New method descriptor
   */
  private static void tryUpdateMethodDescriptor(Context context, ClassWrapper classWrapper, String name, String oldDesc, String newDesc) {
    // Search superclass
    if (classWrapper.classNode().superName != null) {
      ClassWrapper superClass = context.getClassesMap().get(classWrapper.classNode().superName);
      if (superClass != null) {
        tryUpdateMethodDescriptor(context, superClass, name, oldDesc, newDesc);
      }
    }

    // Search interfaces
    classWrapper.classNode().interfaces.forEach(interfaceName -> {
      ClassWrapper interfaceClass = context.getClassesMap().get(interfaceName);
      if (interfaceClass != null) {
        tryUpdateMethodDescriptor(context, interfaceClass, name, oldDesc, newDesc);
      }
    });

    Optional<MethodNode> optMethodNode = classWrapper.classNode().methods.stream()
        .filter(method -> method.name.equals(name) && method.desc.equals(oldDesc))
        .findFirst();

    // Update method descriptor
    optMethodNode.ifPresent(methodNode -> methodNode.desc = newDesc);
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

  public static void removeField(FieldInsnNode fieldInsnNode, Context context) {
    if (!context.getClassesMap().containsKey(fieldInsnNode.owner)) return;

    context
        .getClassesMap()
        .get(fieldInsnNode.owner)
        .fields()
        .removeIf(
            fieldNode ->
                fieldNode.name.equals(fieldInsnNode.name)
                    && fieldNode.desc.equals(fieldInsnNode.desc));
  }

  public static Optional<MethodNode> findMethod(ClassNode classNode, MethodRef methodRef) {
    if (classNode == null || classNode.methods == null) {
      return Optional.empty();
    }
    return classNode.methods.stream()
        .filter(methodNode -> methodNode.name.equals(methodRef.name()))
        .filter(methodNode -> methodNode.desc.equals(methodRef.desc()))
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
}
