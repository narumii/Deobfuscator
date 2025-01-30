package uwu.narumi.deobfuscator.api.asm;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;

public class MethodlessInsnContext extends InsnContext {
  private final Frame<OriginalSourceValue> frame;

  public MethodlessInsnContext(AbstractInsnNode insn, Frame<OriginalSourceValue> frame) {
    super(insn, null);
    this.frame = frame;
  }

  @Override
  public Frame<OriginalSourceValue> frame() {
    return this.frame;
  }

  @Override
  public MethodNode methodNode() {
    throw new UnsupportedOperationException();
  }

  @Override
  public InsnContext of(AbstractInsnNode insn) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MethodContext methodContext() {
    throw new UnsupportedOperationException();
  }
}
