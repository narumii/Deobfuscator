package uwu.narumi.deobfuscator.api.asm.matcher.impl;

import org.objectweb.asm.tree.TypeInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

public class TypeMatch extends Match {

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
  protected boolean test(MatchContext context) {
    return context.insn() instanceof TypeInsnNode typeInsn
        && (opcode == -1 || typeInsn.getOpcode() == opcode)
        && (desc == null || typeInsn.desc.equals(desc));
  }
}
