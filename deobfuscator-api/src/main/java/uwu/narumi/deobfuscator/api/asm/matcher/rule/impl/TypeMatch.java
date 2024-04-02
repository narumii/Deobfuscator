package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class TypeMatch implements Match {

  private final int opcode;
  private final String desc;

  private TypeMatch(int opcode, String desc) {
    this.opcode = opcode;
    this.desc = desc;
  }

  public static TypeMatch of(int opcode, String desc) {
    return new TypeMatch(opcode, desc);
  }

  public static TypeMatch of(String desc) {
    return of(-1, desc);
  }

  public static TypeMatch of(int opcode) {
    return of(opcode, null);
  }

  public static TypeMatch of() {
    return of(-1, null);
  }

  @Override
  public boolean test(AbstractInsnNode node) {
    return node instanceof TypeInsnNode
        && (opcode == -1 || node.getOpcode() == opcode)
        && (desc == null || ((TypeInsnNode) node).desc.equals(desc));
  }
}
