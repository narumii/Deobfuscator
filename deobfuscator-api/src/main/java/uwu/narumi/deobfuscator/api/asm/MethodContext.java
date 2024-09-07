package uwu.narumi.deobfuscator.api.asm;

import org.jetbrains.annotations.Unmodifiable;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;

import java.util.Map;

/**
 * Method context
 *
 * @param classWrapper Class that owns this instruction
 * @param methodNode Method that owns this instruction
 * @param frames Frames of the method
 */
public record MethodContext(
    ClassWrapper classWrapper,
    MethodNode methodNode,
    @Unmodifiable Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames
) {

  public static MethodContext create(ClassWrapper classWrapper, MethodNode methodNode) {
    Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames = AsmHelper.analyzeSource(classWrapper.getClassNode(), methodNode);
    return new MethodContext(classWrapper, methodNode, frames);
  }

  public InstructionContext createInsnContext(AbstractInsnNode insn) {
    return new InstructionContext(insn, this);
  }
}
