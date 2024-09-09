package uwu.narumi.deobfuscator.api.asm.matcher.impl;

import java.util.function.Predicate;

import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

public class InvokeDynamicMatch extends Match {
  private String name;
  private String desc;

  private String bsmOwner;
  private String bsmName;
  private String bsmDesc;
  private int bsmTag = -1;

  private Predicate<Object[]> bsmArgsPredicate;

  private InvokeDynamicMatch() {}

  public static InvokeDynamicMatch create() {
    return new InvokeDynamicMatch();
  }

  @Override
  protected boolean test(MatchContext context) {
    return context.insn() instanceof InvokeDynamicInsnNode invokeDynamicInsn
        && (name == null || invokeDynamicInsn.name.equals(name))
        && (desc == null || invokeDynamicInsn.desc.equals(desc))
        && (bsmOwner == null || invokeDynamicInsn.bsm.getOwner().equals(bsmOwner))
        && (bsmName == null || invokeDynamicInsn.bsm.getName().equals(bsmName))
        && (bsmDesc == null || invokeDynamicInsn.bsm.getDesc().equals(bsmDesc))
        && (bsmTag == -1 || invokeDynamicInsn.bsm.getTag() == bsmTag)
        && (bsmArgsPredicate == null
            || bsmArgsPredicate.test(invokeDynamicInsn.bsmArgs));
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

  public static class Pred extends Match {
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
    protected boolean test(MatchContext context) {
      return context.insn() instanceof InvokeDynamicInsnNode invokeDynamicInsn
          && (name == null || name.test(invokeDynamicInsn.name))
          && (desc == null || desc.test(invokeDynamicInsn.desc))
          && (bsmOwner == null || bsmOwner.test(invokeDynamicInsn.bsm.getOwner()))
          && (bsmName == null || bsmName.test(invokeDynamicInsn.bsm.getName()))
          && (bsmDesc == null || bsmDesc.test(invokeDynamicInsn.bsm.getDesc()))
          && (bsmTag == null || bsmTag.test(invokeDynamicInsn.bsm.getTag()))
          && (bsmArgsPredicate == null || bsmArgsPredicate.test(invokeDynamicInsn.bsmArgs));
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
