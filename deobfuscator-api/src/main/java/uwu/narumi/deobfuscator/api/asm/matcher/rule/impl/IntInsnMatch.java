package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class IntInsnMatch implements Match {

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
  public boolean test(AbstractInsnNode node) {
    return node instanceof IntInsnNode
        && node.getOpcode() == opcode
        && ((IntInsnNode) node).operand == operand;
  }
}
