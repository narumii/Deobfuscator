package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class RangeOpcodeMatch implements Match {
  private final int start;
  private final int end;

  private RangeOpcodeMatch(int start, int end) {
    this.start = start;
    this.end = end;
  }

  public static RangeOpcodeMatch of(int startingOpcode, int endingOpcode) {
    return new RangeOpcodeMatch(startingOpcode, endingOpcode);
  }

  @Override
  public boolean test(AbstractInsnNode node) {
    return node != null && node.getOpcode() >= start && node.getOpcode() <= end;
  }
}
