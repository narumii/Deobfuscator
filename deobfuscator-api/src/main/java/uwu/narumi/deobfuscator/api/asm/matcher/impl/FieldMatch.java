package uwu.narumi.deobfuscator.api.asm.matcher.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

public class FieldMatch extends Match {

  private final int opcode;
  private String owner;
  private String name;
  private String desc;

  private FieldMatch(int opcode) {
    this.opcode = opcode;
  }

  public static FieldMatch of(int opcode) {
    return new FieldMatch(opcode);
  }

  public static FieldMatch create() {
    return new FieldMatch(-1);
  }

  public static FieldMatch putStatic() {
    return new FieldMatch(Opcodes.PUTSTATIC);
  }

  public static FieldMatch getStatic() {
    return new FieldMatch(Opcodes.GETSTATIC);
  }

  public static FieldMatch putField() {
    return new FieldMatch(Opcodes.PUTFIELD);
  }

  public static FieldMatch getField() {
    return new FieldMatch(Opcodes.GETFIELD);
  }

  @Override
  protected boolean test(MatchContext context) {
    return context.insn() instanceof FieldInsnNode fieldInsn
        && (opcode == -1 || fieldInsn.getOpcode() == opcode)
        && (owner == null || fieldInsn.owner.equals(owner))
        && (name == null || fieldInsn.name.equals(name))
        && (desc == null || fieldInsn.desc.equals(desc));
  }

  public FieldMatch owner(String owner) {
    this.owner = owner;
    return this;
  }

  public FieldMatch name(String name) {
    this.name = name;
    return this;
  }

  public FieldMatch desc(String desc) {
    this.desc = desc;
    return this;
  }
}
