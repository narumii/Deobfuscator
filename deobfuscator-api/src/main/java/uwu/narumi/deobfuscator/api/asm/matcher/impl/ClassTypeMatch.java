package uwu.narumi.deobfuscator.api.asm.matcher.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

/**
 * Matches by instruction class type
 */
public class ClassTypeMatch extends Match {

  private final Class<? extends AbstractInsnNode> classType;

  private ClassTypeMatch(Class<? extends AbstractInsnNode> classType) {
    this.classType = classType;
  }

  public static ClassTypeMatch of(Class<? extends AbstractInsnNode> classType) {
    return new ClassTypeMatch(classType);
  }

  @Override
  protected boolean test(MatchContext context) {
    return context.insn().getClass().equals(classType);
  }
}
