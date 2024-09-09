package uwu.narumi.deobfuscator.api.asm.matcher.impl;

import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

public class RangeOpcodeMatch extends Match {
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
  protected boolean test(MatchContext context) {
    return context.insn().getOpcode() >= start && context.insn().getOpcode() <= end;
  }
}
