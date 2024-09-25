package uwu.narumi.deobfuscator.api.asm.matcher.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

/**
 * Matches an instruction by its instance.
 */
public class InsnMatch extends Match {

  private final AbstractInsnNode node;

  private InsnMatch(AbstractInsnNode node) {
    this.node = node;
  }

  public static InsnMatch of(AbstractInsnNode node) {
    return new InsnMatch(node);
  }

  @Override
  protected boolean test(MatchContext context) {
    return this.node.equals(context.insn());
  }
}
