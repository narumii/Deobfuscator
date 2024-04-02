package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import java.util.function.Predicate;
import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class ClassMatch implements Match {

  private final Class<? extends AbstractInsnNode> clazz;

  private ClassMatch(Class<? extends AbstractInsnNode> clazz) {
    this.clazz = clazz;
  }

  public static ClassMatch of(Class<? extends AbstractInsnNode> clazz) {
    return new ClassMatch(clazz);
  }

  @Override
  public boolean test(AbstractInsnNode node) {
    return node != null && node.getClass().equals(clazz);
  }

  public static class Pred implements Match {
    private final Predicate<Class<? extends AbstractInsnNode>> clazz;

    private Pred(Predicate<Class<? extends AbstractInsnNode>> clazz) {
      this.clazz = clazz;
    }

    public static Pred of(Predicate<Class<? extends AbstractInsnNode>> clazz) {
      return new Pred(clazz);
    }

    @Override
    public boolean test(AbstractInsnNode node) {
      return node != null && clazz.test(node.getClass());
    }
  }
}
