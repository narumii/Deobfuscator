package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import java.util.function.Predicate;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class InvokeDynamicMatch implements Match {
  private String name;
  private String desc;

  private String bsmOwner;
  private String bsmName;
  private String bsmDesc;
  private int bsmTag = -1;

  private Predicate<Object[]> bsmArgsPredicate;

  private Transformation transformation;

  private InvokeDynamicMatch() {}

  public static InvokeDynamicMatch create() {
    return new InvokeDynamicMatch();
  }

  public Match defineTransformation(Transformation transformation) {
    this.transformation = transformation;
    return this;
  }

  @Override
  public Transformation transformation() {
    return this.transformation;
  }

  @Override
  public boolean test(AbstractInsnNode node) {
    return node instanceof InvokeDynamicInsnNode
        && (name == null || ((InvokeDynamicInsnNode) node).name.equals(name))
        && (desc == null || ((InvokeDynamicInsnNode) node).desc.equals(desc))
        && (bsmOwner == null || ((InvokeDynamicInsnNode) node).bsm.getOwner().equals(bsmOwner))
        && (bsmName == null || ((InvokeDynamicInsnNode) node).bsm.getName().equals(bsmName))
        && (bsmDesc == null || ((InvokeDynamicInsnNode) node).bsm.getDesc().equals(bsmDesc))
        && (bsmTag == -1 || ((InvokeDynamicInsnNode) node).bsm.getTag() == bsmTag)
        && (bsmArgsPredicate == null
            || bsmArgsPredicate.test(((InvokeDynamicInsnNode) node).bsmArgs));
  }

  public InvokeDynamicMatch name(String name) {
    this.name = name;
    return this;
  }

  public InvokeDynamicMatch desc(String desc) {
    this.desc = desc;
    return this;
  }

  public InvokeDynamicMatch bsmOwner(String bsmOwner) {
    this.bsmOwner = bsmOwner;
    return this;
  }

  public InvokeDynamicMatch bsmName(String bsmName) {
    this.bsmName = bsmName;
    return this;
  }

  public InvokeDynamicMatch bsmDesc(String bsmDesc) {
    this.bsmDesc = bsmDesc;
    return this;
  }

  public InvokeDynamicMatch bsmTag(int bsmTag) {
    this.bsmTag = bsmTag;
    return this;
  }

  public InvokeDynamicMatch bsmArgsPredicate(Predicate<Object[]> predicate) {
    this.bsmArgsPredicate = predicate;
    return this;
  }

  public static class Pred implements Match {
    private Predicate<String> name;
    private Predicate<String> desc;

    private Predicate<String> bsmOwner;
    private Predicate<String> bsmName;
    private Predicate<String> bsmDesc;
    private Predicate<Integer> bsmTag;

    private Predicate<Object[]> bsmArgsPredicate;

    private Pred() {}

    public static Pred create() {
      return new Pred();
    }

    @Override
    public boolean test(AbstractInsnNode node) {
      return node instanceof InvokeDynamicInsnNode
          && (name == null || name.test(((InvokeDynamicInsnNode) node).name))
          && (desc == null || desc.test(((InvokeDynamicInsnNode) node).desc))
          && (bsmOwner == null || bsmOwner.test(((InvokeDynamicInsnNode) node).bsm.getOwner()))
          && (bsmName == null || bsmName.test(((InvokeDynamicInsnNode) node).bsm.getName()))
          && (bsmDesc == null || bsmDesc.test(((InvokeDynamicInsnNode) node).bsm.getDesc()))
          && (bsmTag == null || bsmTag.test(((InvokeDynamicInsnNode) node).bsm.getTag()))
          && (bsmArgsPredicate == null
              || bsmArgsPredicate.test(((InvokeDynamicInsnNode) node).bsmArgs));
    }

    public Pred name(Predicate<String> name) {
      this.name = name;
      return this;
    }

    public Pred desc(Predicate<String> desc) {
      this.desc = desc;
      return this;
    }

    public Pred bsmOwner(Predicate<String> bsmOwner) {
      this.bsmOwner = bsmOwner;
      return this;
    }

    public Pred bsmName(Predicate<String> bsmName) {
      this.bsmName = bsmName;
      return this;
    }

    public Pred bsmDesc(Predicate<String> bsmDesc) {
      this.bsmDesc = bsmDesc;
      return this;
    }

    public Pred bsmTag(Predicate<Integer> bsmTag) {
      this.bsmTag = bsmTag;
      return this;
    }

    public Pred bsmArgsPredicate(Predicate<Object[]> predicate) {
      this.bsmArgsPredicate = predicate;
      return this;
    }
  }
}
