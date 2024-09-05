package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import org.objectweb.asm.tree.IntInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.MatchContext;

public class IntInsnMatch extends Match {

  private final int opcode;
  private final int operand;

  private IntInsnMatch(int opcode, int operand) {
    this.opcode = opcode;
    this.operand = operand;
  }

  public static IntInsnMatch of(int opcode, int operand) {
    return new IntInsnMatch(opcode, operand);
  }

  @Override
  protected boolean test(MatchContext context) {
    return context.insn() instanceof IntInsnNode intInsn
        && intInsn.getOpcode() == opcode
        && intInsn.operand == operand;
  }
}
