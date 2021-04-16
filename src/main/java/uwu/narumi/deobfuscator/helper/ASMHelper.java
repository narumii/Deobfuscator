package uwu.narumi.deobfuscator.helper;

import java.util.Optional;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Used: https://github.com/ItzSomebody/radon/blob/master/src/main/java/me/itzsomebody/radon/utils/ASMUtils.java
 */
public class ASMHelper implements Opcodes {

  public boolean isReturn(AbstractInsnNode node) {
    if (node == null) {
      return false;
    }

    return node.getOpcode() >= 172 && node.getOpcode() <= 177;
  }

  public static boolean isString(AbstractInsnNode node) {
    return node instanceof LdcInsnNode && ((LdcInsnNode) node).cst instanceof String;
  }

  public static String getStringFromNode(AbstractInsnNode node) {
    return (String) ((LdcInsnNode) node).cst;
  }

  public static boolean isInteger(AbstractInsnNode insn) {
    if (insn == null) {
      return false;
    }

    int opcode = insn.getOpcode();
    return ((opcode >= ICONST_M1 && opcode <= ICONST_5)
        || opcode == BIPUSH
        || opcode == SIPUSH
        || (insn instanceof LdcInsnNode
        && ((LdcInsnNode) insn).cst instanceof Integer));
  }

  public static boolean isLong(AbstractInsnNode insn) {
    if (insn == null) {
      return false;
    }

    int opcode = insn.getOpcode();
    return (opcode == LCONST_0
        || opcode == LCONST_1
        || (insn instanceof LdcInsnNode
        && ((LdcInsnNode) insn).cst instanceof Long));
  }

  public static boolean isFloat(AbstractInsnNode insn) {
    int opcode = insn.getOpcode();
    return (opcode >= FCONST_0 && opcode <= FCONST_2)
        || (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof Float);
  }

  public static boolean isDouble(AbstractInsnNode insn) {
    int opcode = insn.getOpcode();
    return (opcode >= DCONST_0 && opcode <= DCONST_1)
        || (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof Double);
  }

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
    if (number >= 0 && number <= 2) {
      return new InsnNode((int) (number + 11));
    } else {
      return new LdcInsnNode(number);
    }
  }

  public static AbstractInsnNode getNumber(double number) {
    if (number >= 0 && number <= 1) {
      return new InsnNode((int) (number + 14));
    } else {
      return new LdcInsnNode(number);
    }
  }

  public static int getInteger(AbstractInsnNode insn) {
    int opcode = insn.getOpcode();

    if (opcode >= ICONST_M1 && opcode <= ICONST_5) {
      return opcode - 3;
    } else if (insn instanceof IntInsnNode
        && insn.getOpcode() != NEWARRAY) {
      return ((IntInsnNode) insn).operand;
    } else if (insn instanceof LdcInsnNode
        && ((LdcInsnNode) insn).cst instanceof Integer) {
      return (Integer) ((LdcInsnNode) insn).cst;
    }

    return 0;
  }

  public static long getLong(AbstractInsnNode insn) {
    int opcode = insn.getOpcode();

    if (opcode >= LCONST_0 && opcode <= LCONST_1) {
      return opcode - 9;
    } else if (insn instanceof LdcInsnNode
        && ((LdcInsnNode) insn).cst instanceof Long) {
      return (Long) ((LdcInsnNode) insn).cst;
    }

    return 0;
  }

  public static float getFloat(AbstractInsnNode insn) {
    int opcode = insn.getOpcode();

    if (opcode >= FCONST_0 && opcode <= FCONST_2) {
      return opcode - 11;
    } else if (insn instanceof LdcInsnNode
        && ((LdcInsnNode) insn).cst instanceof Float) {
      return (Float) ((LdcInsnNode) insn).cst;
    }

    return 0;
  }

  public static double getDouble(AbstractInsnNode insn) {
    int opcode = insn.getOpcode();

    if (opcode >= DCONST_0 && opcode <= DCONST_1) {
      return opcode - 14;
    } else if (insn instanceof LdcInsnNode
        && ((LdcInsnNode) insn).cst instanceof Double) {
      return (Double) ((LdcInsnNode) insn).cst;
    }

    return 0;
  }

  public static boolean isNumberOperator(AbstractInsnNode node) {
    return node != null && (node.getOpcode() >= IADD && node.getOpcode() <= LXOR)
        && node.getOpcode() != INEG;
  }

  public static boolean isOpcode(AbstractInsnNode node, int opcode) {
    return node != null && node.getOpcode() == opcode;
  }

  public static boolean isAccess(int access, int opcode) {
    return (access & opcode) != 0;
  }

  public static boolean startWithUTF(String string) {
    return string.toCharArray()[0] > 127 || string.toCharArray()[0] == '\u0000';
  }

  public static int doOperation(int first, int second, int operator) {
    switch (operator) {
      case IADD:
        return first + second;
      case ISUB:
        return first - second;
      case IMUL:
        return first * second;
      case IDIV:
        return first / second;
      case IREM:
        return first % second;
      case ISHL:
        return first << second;
      case ISHR:
        return first >> second;
      case IUSHR:
        return first >>> second;
      case IAND:
        return first & second;
      case IOR:
        return first | second;
      case IXOR:
        return first ^ second;
      default:
        return 0;
    }
  }

  public static boolean has(int access, int opcode) {
    return (access & opcode) != 0;
  }

  public static Optional<MethodNode> findCLInit(ClassNode classNode) {
    return classNode.methods.stream()
        .filter(methodNode -> methodNode.name.equalsIgnoreCase("<clinit>")).findAny();
  }

  public static Optional<MethodNode> findMethod(String name, ClassNode classNode) {
    return classNode.methods.stream().filter(methodNode -> methodNode.name.equals(name)).findAny();
  }

  public static Optional<FieldNode> findField(String name, ClassNode classNode) {
    return classNode.fields.stream().filter(fieldNode -> fieldNode.name.equals(name)).findAny();
  }
}
