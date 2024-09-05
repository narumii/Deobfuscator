package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.MatchContext;

public class OpcodeMatch extends Match {
  private final int opcode;

  private OpcodeMatch(int opcode) {
    this.opcode = opcode;
  }

  public static OpcodeMatch of(int opcode) {
    return new OpcodeMatch(opcode);
  }

  @Override
  protected boolean test(MatchContext context) {
    return context.insn().getOpcode() == opcode;
  }
}
