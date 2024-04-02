package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class InsnMatch implements Match {

  private final AbstractInsnNode node;

  private InsnMatch(AbstractInsnNode node) {
    this.node = node;
  }

  public static InsnMatch of(AbstractInsnNode node) {
    return new InsnMatch(node);
  }

  @Override
  public boolean test(AbstractInsnNode node) {
    return this.node.equals(node);
  }
}
