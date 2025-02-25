package uwu.narumi.deobfuscator.api.asm;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.helper.MethodHelper;

import java.util.Map;
import java.util.Set;

/**
 * Method context
 */
public class MethodContext {
  private final ClassWrapper classWrapper;
  private final MethodNode methodNode;
  private final @Nullable @Unmodifiable Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames;
  private Map<AbstractInsnNode, Set<AbstractInsnNode>> consumersMap = null;

  private MethodContext(
      ClassWrapper classWrapper,
      MethodNode methodNode,
      @Nullable @Unmodifiable Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames
  ) {
    this.classWrapper = classWrapper;
    this.methodNode = methodNode;
    this.frames = frames;
  }

  /**
   * Class that owns this method
   */
  public ClassWrapper classWrapper() {
    return classWrapper;
  }

  /**
   * Method itself
   */
  public MethodNode methodNode() {
    return methodNode;
  }

  /**
   * Frames of the method
   */
  public @Nullable @Unmodifiable Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames() {
    return frames;
  }

  public synchronized Map<AbstractInsnNode, Set<AbstractInsnNode>> getConsumersMap() {
    if (this.frames == null) {
      throw new IllegalStateException("Got frameless method context");
    }
    if (consumersMap == null) {
      // Lazy initialization
      this.consumersMap = MethodHelper.computeConsumersMap(this.frames);
    }
    return consumersMap;
  }

  public InsnContext newInsnContext(AbstractInsnNode insn) {
    return new InsnContext(insn, this);
  }

  public static MethodContext framed(ClassWrapper classWrapper, MethodNode methodNode) {
    return framed(classWrapper, methodNode, MethodHelper::analyzeSource);
  }

  /**
   * Creates new {@link MethodContext} and computes its frames
   */
  public static MethodContext framed(ClassWrapper classWrapper, MethodNode methodNode, FramesProvider framesProvider) {
    Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames = framesProvider.compute(classWrapper.classNode(), methodNode);
    return new MethodContext(classWrapper, methodNode, frames);
  }

  public static MethodContext frameless(ClassWrapper classWrapper, MethodNode methodNode) {
    return new MethodContext(classWrapper, methodNode, null);
  }
}
