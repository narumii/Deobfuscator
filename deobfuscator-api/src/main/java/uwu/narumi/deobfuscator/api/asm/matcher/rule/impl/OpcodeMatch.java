package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class OpcodeMatch implements Match {
  private final int opcode;

  private OpcodeMatch(int opcode) {
    this.opcode = opcode;
  }

  public static OpcodeMatch of(int opcode) {
    return new OpcodeMatch(opcode);
  }

  @Override
  public boolean test(AbstractInsnNode node) {
    return node != null && node.getOpcode() == opcode;
  }
}
