package uwu.narumi.deobfuscator.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class InstructionMatcher {

    private final int[] opcodes;
    private int[] replacement;

    private InstructionMatcher(int... opcodes) {
        if (opcodes == null || opcodes.length <= 0)
            throw new IllegalArgumentException();

        this.opcodes = opcodes;
    }

    public static InstructionMatcher of(int... opcodes) {
        return new InstructionMatcher(opcodes);
    }

    public InstructionMatcher replacement(int... opcodes) {
        this.replacement = opcodes;
        return this;
    }

    public boolean match(AbstractInsnNode start) {
        int success = 0;
        AbstractInsnNode current = start;

        for (int i = 0; i < opcodes.length; i++) {
            if (current == null)
                break;

            //Idk
            if (current instanceof LabelNode || current instanceof LineNumberNode || current.getOpcode() == Opcodes.NOP) {
                i--;
            } else if (opcodes[i] != current.getOpcode())
                break;
            else if (opcodes[i] == current.getOpcode())
                ++success;

            current = current.getNext();
        }

        return success == opcodes.length;
    }

    public AbstractInsnNode matchAndGetLast(AbstractInsnNode start) {
        int success = 0;
        AbstractInsnNode current = start;

        for (int i = 0; i < opcodes.length; i++) {
            if (current == null)
                break;

            //Idk
            if (current instanceof LabelNode || current instanceof LineNumberNode || current.getOpcode() == Opcodes.NOP) {
                i--;
            } else if (opcodes[i] != current.getOpcode())
                break;
            else if (opcodes[i] == current.getOpcode())
                ++success;

            current = current.getNext();
        }

        return success == opcodes.length ? current : null;
    }


    public boolean matchAndReplace(MethodNode methodNode, AbstractInsnNode start) {
        AbstractInsnNode last = matchAndGetLast(start);
        if (last != null) {
            if (replacement != null && replacement.length > 0) {
                for (int opcode : replacement) {
                    methodNode.instructions.insertBefore(start, new InsnNode(opcode));
                }
            }

            AbstractInsnNode[] nodes = methodNode.instructions.toArray();
            int startIndex = methodNode.instructions.indexOf(start);
            int endIndex = methodNode.instructions.indexOf(last);
            for (int i = startIndex; i < endIndex; i++) {
                methodNode.instructions.remove(nodes[i]);
            }
            return true;
        }

        return false;
    }
}
