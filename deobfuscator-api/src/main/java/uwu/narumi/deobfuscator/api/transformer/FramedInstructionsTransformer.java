package uwu.narumi.deobfuscator.api.transformer;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Transformer that will iterate instructions along with their current {@link Frame}s
 */
public abstract class FramedInstructionsTransformer extends Transformer {
  private AtomicInteger changed = new AtomicInteger(0);

  /**
   * Transform instruction
   *
   * @param classWrapper Current class
   * @param methodNode   Current method
   * @param insn         Current instruction
   * @param frame        Current frame
   * @return If changed
   */
  protected abstract boolean transformInstruction(ClassWrapper classWrapper, MethodNode methodNode, AbstractInsnNode insn, Frame<OriginalSourceValue> frame);

  /**
   * Returns classes stream on which the transformer will be iterating
   */
  protected Stream<ClassWrapper> getClassesStream(Stream<ClassWrapper> stream) {
    // Override this method to filter classes
    return stream;
  }

  /**
   * Returns methods stream on which the transformer will be iterating
   */
  protected Stream<MethodNode> getMethodsStream(Stream<MethodNode> stream) {
    // Override this method to filter methods
    return stream;
  }

  /**
   * Returns instructions stream on which the transformer will be iterating
   */
  protected Stream<AbstractInsnNode> getInstructionsStream(Stream<AbstractInsnNode> stream) {
    // Override this method to filter instructions
    return stream;
  }

  @Override
  protected boolean transform(ClassWrapper scope, Context context) throws Exception {
    getClassesStream(context.classes(scope).stream()).forEach(classWrapper -> getMethodsStream(classWrapper.methods().stream())
        .forEach(methodNode -> {
          // Skip if no instructions
          if (getInstructionsStream(Arrays.stream(methodNode.instructions.toArray())).findAny().isEmpty()) return;

          // Get frames of the method
          Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames = analyzeOriginalSource(classWrapper.getClassNode(), methodNode);
          if (frames == null) return;

          // Iterate over instructions
          getInstructionsStream(Arrays.stream(methodNode.instructions.toArray())).forEach(insn -> {
            // Get current frame
            Frame<OriginalSourceValue> frame = frames.get(insn);
            if (frame == null) return;

            // Run the instruction transformer
            boolean transformerChanged = transformInstruction(classWrapper, methodNode, insn, frame);
            if (transformerChanged) {
              changed.incrementAndGet();
            }
          });
        }));

    LOGGER.info("Transformed {} instructions", changed.get());
    return changed.get() > 0;
  }
}
