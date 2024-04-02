package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class JumpMatch implements Match {

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
  public boolean test(AbstractInsnNode node) {
    return node instanceof JumpInsnNode
        && (opcode == -1 || node.getOpcode() == opcode)
        && (labelNode == null || ((JumpInsnNode) node).label.equals(labelNode));
  }
}
