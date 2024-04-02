package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class DoubleMatch implements Match {

  private static final Match EMPTY = AbstractInsnNode::isDouble;

  private final double number;

  public DoubleMatch(double number) {
    this.number = number;
  }

  public static DoubleMatch of(double number) {
    return new DoubleMatch(number);
  }

  public static Match of() {
    return EMPTY;
  }

  @Override
  public boolean test(AbstractInsnNode node) {
    return node.isDouble() && node.asDouble() == number;
  }
}
