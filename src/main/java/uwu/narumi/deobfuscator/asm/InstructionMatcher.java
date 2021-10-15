package uwu.narumi.deobfuscator.asm;

import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.function.Predicate;

public class InstructionMatcher {

    protected final AbstractInsnNode start;
    protected final Predicate<AbstractInsnNode>[] predicates;
    protected AbstractInsnNode current;

    public InstructionMatcher(AbstractInsnNode start, Predicate<AbstractInsnNode>... predicates) {
        this.start = start;
        this.current = start;
        this.predicates = predicates;
    }

    public boolean matches() {
        int passed = 0;
        for (Predicate<AbstractInsnNode> predicate : predicates) {
            if (current == null)
                break;

            if (predicate.test(current))
                passed++;

            current = current.getNext();
        }

        return passed == predicates.length;
    }

    public AbstractInsnNode getCurrent() {
        return current;
    }
}
