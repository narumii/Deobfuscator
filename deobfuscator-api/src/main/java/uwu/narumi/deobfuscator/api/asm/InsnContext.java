package uwu.narumi.deobfuscator.api.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;

/**
 * Instruction context. Holds all information relevant to the current instruction.
 */
public class InsnContext {
  private final AbstractInsnNode insn;
  private final MethodContext methodContext;

  InsnContext(AbstractInsnNode insn, MethodContext methodContext) {
    this.insn = insn;
    this.methodContext = methodContext;
  }

  public InsnContext of(AbstractInsnNode insn) {
    return new InsnContext(insn, this.methodContext);
  }

  public Frame<OriginalSourceValue> frame() {
    if (this.methodContext.frames() == null) {
      throw new IllegalStateException("Got frameless method context");
    }
    return this.methodContext.frames().get(this.insn);
  }

  public MethodNode methodNode() {
    return this.methodContext.methodNode();
  }

  /**
   * Current instruction
   */
  public AbstractInsnNode insn() {
    return insn;
  }

  /**
   * Method context
   */
  public MethodContext methodContext() {
    return methodContext;
  }

  /**
   * Pops current instruction's stack values by adding POP instructions before this instruction
   *
   * @param count Stack values count to pop
   */
  public void pop(int count) {
    for (int i = 0; i < count; i++) {
      int stackValueIdx = frame().getStackSize() - (i + 1);
      OriginalSourceValue sourceValue = frame().getStack(stackValueIdx);

      // Pop
      InsnNode popInsn = sourceValue.getSize() == 2 ? new InsnNode(Opcodes.POP2) : new InsnNode(Opcodes.POP);
      this.methodNode().instructions.insertBefore(this.insn, popInsn);
    }
  }
}
