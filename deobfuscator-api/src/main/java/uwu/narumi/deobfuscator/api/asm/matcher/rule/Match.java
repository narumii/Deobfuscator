package uwu.narumi.deobfuscator.api.asm.matcher.rule;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;

@FunctionalInterface
public interface Match {

  boolean test(AbstractInsnNode node);

  default Match offset(int offset) {
    return (node) -> test(offset < 0 ? node.getPrevious(Math.abs(offset)) : node.getNext(offset));
  }

  default Match and(Match match) {
    return (node) -> test(node) && match.test(node);
  }

  default Match or(Match match) {
    return (node) -> test(node) || match.test(node);
  }

  default Match not() {
    return (node) -> !test(node);
  }

  default Transformation transformation() {
    return ((methodNode, insn, frame) -> false);
  }

  @FunctionalInterface
  interface Transformation {
    /**
     * Executes given action
     *
     * @param methodNode Method node
     * @param insn Instruction
     * @param frame Current frame. Useful when you need to get some values from the stack.
     * @return If changed
     */
    boolean transform(MethodNode methodNode, AbstractInsnNode insn, Frame<OriginalSourceValue> frame);
  }
}
