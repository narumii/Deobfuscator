package uwu.narumi.deobfuscator.api.transformer;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.InstructionContext;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.context.Context;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Transformer that will iterate instructions along with their current {@link Frame}s
 */
public abstract class FramedInstructionsTransformer extends Transformer {

  /**
   * Transform instruction. DO NOT use {@link Transformer#markChange()} as you need to pass here as
   * a return if changed something
   *
   * @param context
   * @param insnContext Current instruction context
   * @return If changed
   */
  protected abstract boolean transformInstruction(Context context, InstructionContext insnContext);

  /**
   * Returns classes stream on which the transformer will be iterating
   */
  protected Stream<ClassWrapper> buildClassesStream(Stream<ClassWrapper> stream) {
    // Override this method to filter classes
    return stream;
  }

  /**
   * Returns methods stream on which the transformer will be iterating
   */
  protected Stream<MethodNode> buildMethodsStream(Stream<MethodNode> stream) {
    // Override this method to filter methods
    return stream;
  }

  /**
   * Returns instructions stream on which the transformer will be iterating
   */
  protected Stream<AbstractInsnNode> buildInstructionsStream(Stream<AbstractInsnNode> stream) {
    // Override this method to filter instructions
    return stream;
  }

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    buildClassesStream(context.classes(scope).stream()).forEach(classWrapper -> buildMethodsStream(classWrapper.methods().stream())
        .forEach(methodNode -> {
          // Skip if no instructions
          if (buildInstructionsStream(Arrays.stream(methodNode.instructions.toArray())).findAny().isEmpty()) return;

          // Get frames of the method
          MethodContext methodContext = MethodContext.framed(classWrapper, methodNode);

          // Iterate over instructions
          buildInstructionsStream(Arrays.stream(methodNode.instructions.toArray())).forEach(insn -> {
            InstructionContext insnContext = new InstructionContext(insn, methodContext);

            // Check if frame exists
            if (insnContext.frame() == null) return;

            // Run the instruction transformer
            boolean transformerChanged = transformInstruction(context, insnContext);
            if (transformerChanged) {
              this.markChange();
            }
          });
        }));

    LOGGER.info("Transformed {} instructions", this.getChangesCount());
  }
}
