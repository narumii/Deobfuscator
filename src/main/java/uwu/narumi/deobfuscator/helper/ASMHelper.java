package uwu.narumi.deobfuscator.helper;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Used: https://github.com/ItzSomebody/radon/blob/master/src/main/java/me/itzsomebody/radon/utils/ASMUtils.java
 */
public class ASMHelper implements Opcodes {

    public static boolean isString(AbstractInsnNode node) {
        return node instanceof LdcInsnNode && ((LdcInsnNode) node).cst instanceof String;
    }

    public static boolean isInteger(AbstractInsnNode node) {
        int opcode = node.getOpcode();
        return ((opcode >= ICONST_M1 && opcode <= ICONST_5)
                || opcode == BIPUSH
                || opcode == SIPUSH
                || (node instanceof LdcInsnNode
                && ((LdcInsnNode) node).cst instanceof Integer));
    }

    public static boolean isLong(AbstractInsnNode node) {
        int opcode = node.getOpcode();
        return (opcode == LCONST_0
                || opcode == LCONST_1
                || (node instanceof LdcInsnNode
                && ((LdcInsnNode) node).cst instanceof Long));
    }

    public static boolean isFloat(AbstractInsnNode node) {
        int opcode = node.getOpcode();
        return (opcode >= FCONST_0 && opcode <= FCONST_2)
                || (node instanceof LdcInsnNode && ((LdcInsnNode) node).cst instanceof Float);
    }

    public static boolean isDouble(AbstractInsnNode node) {
        int opcode = node.getOpcode();
        return (opcode >= DCONST_0 && opcode <= DCONST_1)
                || (node instanceof LdcInsnNode && ((LdcInsnNode) node).cst instanceof Double);
    }

    public static String getString(AbstractInsnNode node) {
        return (String) ((LdcInsnNode) node).cst;
    }

    public static int getInteger(AbstractInsnNode node) {
        int opcode = node.getOpcode();

        if (opcode >= ICONST_M1 && opcode <= ICONST_5) {
            return opcode - 3;
        } else if (node instanceof IntInsnNode && node.getOpcode() != NEWARRAY) {
            return ((IntInsnNode) node).operand;
        } else if (node instanceof LdcInsnNode
                && ((LdcInsnNode) node).cst instanceof Integer) {
            return (Integer) ((LdcInsnNode) node).cst;
        }

        throw new IllegalArgumentException();
    }

    public static long getLong(AbstractInsnNode node) {
        int opcode = node.getOpcode();

        if (opcode >= LCONST_0 && opcode <= LCONST_1) {
            return opcode - 9;
        } else if (node instanceof LdcInsnNode
                && ((LdcInsnNode) node).cst instanceof Long) {
            return (Long) ((LdcInsnNode) node).cst;
        }

        throw new IllegalArgumentException();
    }

    public static float getFloat(AbstractInsnNode node) {
        int opcode = node.getOpcode();

        if (opcode >= FCONST_0 && opcode <= FCONST_2) {
            return opcode - 11;
        } else if (node instanceof LdcInsnNode
                && ((LdcInsnNode) node).cst instanceof Float) {
            return (Float) ((LdcInsnNode) node).cst;
        }

        throw new IllegalArgumentException();
    }

    public static double getDouble(AbstractInsnNode node) {
        int opcode = node.getOpcode();

        if (opcode >= DCONST_0 && opcode <= DCONST_1) {
            return opcode - 14;
        } else if (node instanceof LdcInsnNode
                && ((LdcInsnNode) node).cst instanceof Double) {
            return (Double) ((LdcInsnNode) node).cst;
        }

        throw new IllegalArgumentException();
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

    public static boolean isNumberOperator(AbstractInsnNode node) {
        return node != null && (node.getOpcode() >= IADD && node.getOpcode() <= LXOR)
                && node.getOpcode() != INEG;
    }

    public static boolean isAccess(int access, int opcode) {
        return (access & opcode) != 0;
    }

    public static boolean startWithUTF(String string) {
        return string.toCharArray()[0] > 127 || string.toCharArray()[0] == '\u0000';
    }

    public static Optional<MethodNode> findMethod(ClassNode classNode, Predicate<MethodNode> predicate) {
        return classNode.methods == null ? Optional.empty() : classNode.methods.stream()
                .filter(predicate)
                .findFirst();
    }

    public static Optional<MethodNode> findClInit(ClassNode classNode) {
        return findMethod(classNode, methodNode -> methodNode.name.equals("<clinit>"));
    }

    public static List<AbstractInsnNode> getInstructionsBetween(AbstractInsnNode start, AbstractInsnNode end) {
        List<AbstractInsnNode> instructions = new ArrayList<>();

        while (!start.equals(end)) {
            instructions.add(start);
            start = start.getNext();
        }
        instructions.add(end);

        return instructions;
    }
}
