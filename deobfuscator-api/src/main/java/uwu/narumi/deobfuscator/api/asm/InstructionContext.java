package uwu.narumi.deobfuscator.api.asm;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;

import java.util.Map;

/**
 * Instruction context. Holds all information relevant to the current instruction.
 *
 * @param insn Current instruction
 * @param classWrapper Class that owns this instruction
 * @param methodNode Method that owns this instruction
 * @param frames Frames of the method
 */
public record InstructionContext(
    AbstractInsnNode insn,
    ClassWrapper classWrapper,
    MethodNode methodNode,
    Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames
) {

  public InstructionContext of(AbstractInsnNode insn) {
    return new InstructionContext(insn, classWrapper, methodNode, frames);
  }

  public Frame<OriginalSourceValue> frame() {
    return this.frames.get(this.insn);
  }
}
