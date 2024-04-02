package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class LongMatch implements Match {

  private static final Match EMPTY = AbstractInsnNode::isLong;

  private final long number;

  public LongMatch(long number) {
    this.number = number;
  }

  public static LongMatch of(long number) {
    return new LongMatch(number);
  }

  public static Match of() {
    return EMPTY;
  }

  @Override
  public boolean test(AbstractInsnNode node) {
    return node.isLong() && node.asLong() == number;
  }
}
