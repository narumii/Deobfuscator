package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class StringMatch implements Match {

  private static final Match EMPTY = AbstractInsnNode::isString;

  private final String string;

  private StringMatch(String string) {
    this.string = string;
  }

  public static StringMatch of(String string) {
    return new StringMatch(string);
  }

  public static Match of() {
    return EMPTY;
  }

  @Override
  public boolean test(AbstractInsnNode node) {
    return node.isString() && node.asString().equals(string);
  }
}
