package uwu.narumi.deobfuscator.api.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;

/**
 * Instruction context. Holds all information relevant to the current instruction.
 *
 * @param insn Current instruction
 * @param methodContext Method context
 */
public record InstructionContext(
    AbstractInsnNode insn,
    MethodContext methodContext
) {

  public InstructionContext of(AbstractInsnNode insn) {
    return new InstructionContext(insn, this.methodContext);
  }

  public Frame<OriginalSourceValue> frame() {
    return this.methodContext.frames().get(this.insn);
  }

  public MethodNode methodNode() {
    return this.methodContext.methodNode();
  }

  /**
   * Adds POP instruction to pop current instruction.
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
