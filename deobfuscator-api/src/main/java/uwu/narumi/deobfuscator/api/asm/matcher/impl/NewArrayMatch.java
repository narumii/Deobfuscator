package uwu.narumi.deobfuscator.api.asm.matcher.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.IntInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

public class NewArrayMatch extends Match {
  // https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-6.html#jvms-6.5.newarray
  private final int arrayTypeCode; // Example: Opcodes.T_INT

  private NewArrayMatch(int arrayTypeCode) {
    this.arrayTypeCode = arrayTypeCode;
  }

  public static NewArrayMatch of(int operand) {
    return new NewArrayMatch(operand);
  }

  @Override
  protected boolean test(MatchContext context) {
    return context.insn().getOpcode() == Opcodes.NEWARRAY && ((IntInsnNode) context.insn()).operand == this.arrayTypeCode;
  }
}
