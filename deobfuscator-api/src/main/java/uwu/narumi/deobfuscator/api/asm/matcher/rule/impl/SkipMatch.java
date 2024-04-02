package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class SkipMatch implements Match {

  private SkipMatch() {}

  public static SkipMatch create() {
    return new SkipMatch();
  }

  @Override
  public boolean test(AbstractInsnNode node) {
    return true;
  }
}
