package uwu.narumi.deobfuscator.api.asm.matcher.impl;

import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

public class JumpMatch extends Match {

  private final int opcode;
  private final LabelNode labelNode;

  private JumpMatch(int opcode, LabelNode labelNode) {
    this.opcode = opcode;
    this.labelNode = labelNode;
  }

  public static JumpMatch of(int opcode, LabelNode labelNode) {
    return new JumpMatch(opcode, labelNode);
  }

  public static JumpMatch of(LabelNode labelNode) {
    return of(-1, labelNode);
  }

  public static JumpMatch of(int opcode) {
    return of(opcode, null);
  }

  public static JumpMatch of() {
    return of(-1, null);
  }

  @Override
  protected boolean test(MatchContext context) {
    return context.insn() instanceof JumpInsnNode jumpInsn
        && (opcode == -1 || jumpInsn.getOpcode() == opcode)
        && (labelNode == null || jumpInsn.label.equals(labelNode));
  }
}
