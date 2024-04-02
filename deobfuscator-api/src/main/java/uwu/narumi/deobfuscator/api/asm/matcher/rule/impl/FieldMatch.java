package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import java.util.function.BiFunction;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class FieldMatch implements Match {

  private final int opcode;
  private String owner;
  private String name;
  private String desc;

  private BiFunction<MethodNode, AbstractInsnNode, Boolean> action;

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

  public Match invokeAction(BiFunction<MethodNode, AbstractInsnNode, Boolean> function) {
    this.action = function;
    return this;
  }

  @Override
  public boolean invoke(MethodNode methodNode, AbstractInsnNode node) {
    return action.apply(methodNode, node);
  }

  @Override
  public boolean test(AbstractInsnNode node) {
    return node instanceof FieldInsnNode
        && (opcode == -1 || node.getOpcode() == opcode)
        && (owner == null || ((FieldInsnNode) node).owner.equals(owner))
        && (name == null || ((FieldInsnNode) node).name.equals(name))
        && (desc == null || ((FieldInsnNode) node).desc.equals(desc));
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
