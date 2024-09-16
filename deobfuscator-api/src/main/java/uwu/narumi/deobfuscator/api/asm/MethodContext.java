package uwu.narumi.deobfuscator.api.asm;

import org.jetbrains.annotations.Nullable;
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
    @Nullable @Unmodifiable Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames
) {

  /**
   * Creates new {@link MethodContext} and computes its frames
   */
  public static MethodContext framed(ClassWrapper classWrapper, MethodNode methodNode) {
    Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames = AsmHelper.analyzeSource(classWrapper.classNode(), methodNode);
    return new MethodContext(classWrapper, methodNode, frames);
  }

  public static MethodContext frameless(ClassWrapper classWrapper, MethodNode methodNode) {
    return new MethodContext(classWrapper, methodNode, null);
  }

  public InstructionContext newInsnContext(AbstractInsnNode insn) {
    return new InstructionContext(insn, this);
  }
}
