package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class IntegerMatch implements Match {

  private static final Match EMPTY = AbstractInsnNode::isInteger;
  private final int number;

  public IntegerMatch(int number) {
    this.number = number;
  }

  public static IntegerMatch of(int number) {
    return new IntegerMatch(number);
  }

  public static Match of() {
    return EMPTY;
  }

  @Override
  public boolean test(AbstractInsnNode node) {
    return node.isInteger() && node.asInteger() == number;
  }
}
