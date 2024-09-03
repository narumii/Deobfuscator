package uwu.narumi.deobfuscator.api.transformer;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;

import java.util.Map;
import java.util.stream.Stream;

public abstract class FramedMethodsTransformer extends Transformer {

  /**
   * Transform method.
   *
   * @param context Current context
   * @param classWrapper Current class
   * @param methodNode Current method
   * @param frames Frames of the current method
   */
  protected abstract void transformMethod(Context context, ClassWrapper classWrapper, MethodNode methodNode, Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames);

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

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    buildClassesStream(context.classes(scope).stream()).forEach(classWrapper -> buildMethodsStream(classWrapper.methods().stream())
        .forEach(methodNode -> {
          // Get frames of the method
          Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames = analyzeSource(classWrapper.getClassNode(), methodNode);
          if (frames == null) return;

          this.transformMethod(context, classWrapper, methodNode, frames);
        }));
  }
}
