package uwu.narumi.deobfuscator.api.asm.matcher.rule;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

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

  default boolean invoke(MethodNode methodNode, AbstractInsnNode node) {
    return false;
  }
}
