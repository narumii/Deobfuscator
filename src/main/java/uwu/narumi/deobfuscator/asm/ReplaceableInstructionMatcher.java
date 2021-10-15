package uwu.narumi.deobfuscator.asm;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import java.util.function.Predicate;

public class ReplaceableInstructionMatcher extends InstructionMatcher {

    private final MethodNode methodNode;

    public ReplaceableInstructionMatcher(MethodNode methodNode, AbstractInsnNode start, Predicate<AbstractInsnNode>... predicates) {
        super(start, predicates);
        this.methodNode = methodNode;
    }

    public boolean matchAndReplaceFirst(AbstractInsnNode node) {
        if (matches()) {
            methodNode.instructions.set(start, node);
            return true;
        }

        return false;
    }

    public boolean matchAndReplaceLast(AbstractInsnNode node) {
        if (matches()) {
            methodNode.instructions.set(current, node);
            return true;
        }

        return false;
    }

    //Ugly as fuck
    public boolean matchAndReplace(AbstractInsnNode node) {
        if (matches()) {
            AbstractInsnNode current = start;
            AbstractInsnNode[] nodes = new AbstractInsnNode[predicates.length];
            for (int i = 0; i < nodes.length; i++) {
                nodes[i] = current;
                current = current.getNext();
            }

            methodNode.instructions.insertBefore(start, node);
            for (AbstractInsnNode insn : nodes) {
                methodNode.instructions.remove(insn);
            }

            return true;
        }

        return false;
    }

    //Ugly as fuck
    public boolean matchAndReplace(InsnList insnList) {
        if (matches()) {
            AbstractInsnNode current = start;
            AbstractInsnNode[] nodes = new AbstractInsnNode[predicates.length];
            for (int i = 0; i < nodes.length; i++) {
                nodes[i] = current;
                current = current.getNext();
            }

            methodNode.instructions.insertBefore(start, insnList);
            for (AbstractInsnNode insn : nodes) {
                methodNode.instructions.remove(insn);
            }

            return true;
        }

        return false;
    }

    //Ugly as fuck
    public boolean matchAndReplace(AbstractInsnNode... replacement) {
        if (matches()) {
            AbstractInsnNode current = start;
            AbstractInsnNode[] nodes = new AbstractInsnNode[predicates.length];
            for (int i = 0; i < nodes.length; i++) {
                nodes[i] = current;
                current = current.getNext();
            }

            for (AbstractInsnNode node : replacement) {
                methodNode.instructions.insertBefore(start, node);
            }

            for (AbstractInsnNode insn : nodes) {
                methodNode.instructions.remove(insn);
            }

            return true;
        }

        return false;
    }
}
