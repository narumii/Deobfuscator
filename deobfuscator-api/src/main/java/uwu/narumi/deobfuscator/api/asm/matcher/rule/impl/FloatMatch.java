package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class FloatMatch implements Match {

  private static final Match EMPTY = AbstractInsnNode::isFloat;

  private final float number;

  public FloatMatch(float number) {
    this.number = number;
  }

  public static FloatMatch of(float number) {
    return new FloatMatch(number);
  }

  public static Match of() {
    return EMPTY;
  }

  @Override
  public boolean test(AbstractInsnNode node) {
    return node.isFloat() && node.asFloat() == number;
  }
}
