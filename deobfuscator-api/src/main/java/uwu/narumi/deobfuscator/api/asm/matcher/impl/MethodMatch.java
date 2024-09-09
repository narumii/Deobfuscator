package uwu.narumi.deobfuscator.api.asm.matcher.impl;

import java.util.Arrays;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

public class MethodMatch extends Match {

  private final int opcode;
  private String owner;
  private String[] name;
  private String desc;

  private MethodMatch(int opcode) {
    this.opcode = opcode;
  }

  public static MethodMatch of(int opcode) {
    return new MethodMatch(opcode);
  }

  public static MethodMatch create() {
    return new MethodMatch(-1);
  }

  public static MethodMatch invokeStatic() {
    return new MethodMatch(Opcodes.INVOKESTATIC);
  }

  public static MethodMatch invokeVirtual() {
    return new MethodMatch(Opcodes.INVOKEVIRTUAL);
  }

  public static MethodMatch invokeSpecial() {
    return new MethodMatch(Opcodes.INVOKESPECIAL);
  }

  public static MethodMatch invokeInterface() {
    return new MethodMatch(Opcodes.INVOKEINTERFACE);
  }

  @Override
  protected boolean test(MatchContext context) {
    return context.insn() instanceof MethodInsnNode methodInsn
        && (opcode == -1 || methodInsn.getOpcode() == opcode)
        && (owner == null || methodInsn.owner.equals(owner))
        && (name == null || Arrays.binarySearch(name, methodInsn.name) >= 0)
        && (desc == null || methodInsn.desc.equals(desc));
  }

  public MethodMatch owner(String owner) {
    this.owner = owner;
    return this;
  }

  public MethodMatch name(String... name) {
    this.name = name;
    return this;
  }

  public MethodMatch desc(String desc) {
    this.desc = desc;
    return this;
  }
}
