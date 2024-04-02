package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class NonNullMatch implements Match {

  private NonNullMatch() {}

  public static NonNullMatch of() {
    return new NonNullMatch();
  }

  @Override
  public boolean test(AbstractInsnNode node) {
    return node != null;
  }
}
