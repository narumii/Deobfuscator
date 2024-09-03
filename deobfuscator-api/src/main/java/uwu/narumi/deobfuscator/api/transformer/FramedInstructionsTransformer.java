package uwu.narumi.deobfuscator.api.transformer;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Transformer that will iterate instructions along with their current {@link Frame}s
 */
public abstract class FramedInstructionsTransformer extends FramedMethodsTransformer {

  /**
   * Transform instruction. DO NOT use {@link Transformer#markChange()} as you need to pass here as
   * a return if changed something
   *
   * @param context Current context
   * @param classWrapper Current class
   * @param methodNode Current method
   * @param frames Frames of the current method
   * @param insn Current instruction
   * @param frame Current frame
   * @return If changed
   */
  protected abstract boolean transformInstruction(
      Context context,
      ClassWrapper classWrapper,
      MethodNode methodNode,
      Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames,
      AbstractInsnNode insn,
      Frame<OriginalSourceValue> frame
  );

  /**
   * Returns instructions stream on which the transformer will be iterating
   */
  protected Stream<AbstractInsnNode> buildInstructionsStream(Stream<AbstractInsnNode> stream) {
    // Override this method to filter instructions
    return stream;
  }

  @Override
  protected void transformMethod(Context context, ClassWrapper classWrapper, MethodNode methodNode, Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames) {
    buildInstructionsStream(Arrays.stream(methodNode.instructions.toArray())).forEach(insn -> {
      // Get current frame
      Frame<OriginalSourceValue> frame = frames.get(insn);
      if (frame == null) return;

      // Run the instruction transformer
      boolean transformerChanged = transformInstruction(context, classWrapper, methodNode, frames, insn, frame);
      if (transformerChanged) {
        this.markChange();
      }
    });
  }

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    super.transform(scope, context);

    LOGGER.info("Transformed {} instructions", this.getChangesCount());
  }
}
