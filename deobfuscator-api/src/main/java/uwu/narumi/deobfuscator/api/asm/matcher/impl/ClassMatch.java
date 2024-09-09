package uwu.narumi.deobfuscator.api.asm.matcher.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

import java.util.function.Predicate;

public class ClassMatch extends Match {

  private final Class<? extends AbstractInsnNode> clazz;

  private ClassMatch(Class<? extends AbstractInsnNode> clazz) {
    this.clazz = clazz;
  }

  public static ClassMatch of(Class<? extends AbstractInsnNode> clazz) {
    return new ClassMatch(clazz);
  }

  @Override
  protected boolean test(MatchContext context) {
    return context.insn() != null && context.insn().getClass().equals(clazz);
  }

  public static class Pred extends Match {
    private final Predicate<Class<? extends AbstractInsnNode>> clazz;

    private Pred(Predicate<Class<? extends AbstractInsnNode>> clazz) {
      this.clazz = clazz;
    }

    public static Pred of(Predicate<Class<? extends AbstractInsnNode>> clazz) {
      return new Pred(clazz);
    }

    @Override
    protected boolean test(MatchContext context) {
      return context.insn() != null && clazz.test(context.insn().getClass());
    }
  }
}
