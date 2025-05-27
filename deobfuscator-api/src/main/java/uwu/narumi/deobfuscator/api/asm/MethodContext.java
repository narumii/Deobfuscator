package uwu.narumi.deobfuscator.api.asm;

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
  private final FramesProvider framesProvider;
  // Lazily initialized
  private Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames = null;
  // Lazily initialized
  private Map<AbstractInsnNode, Set<AbstractInsnNode>> consumersMap = null;

  private MethodContext(
      ClassWrapper classWrapper,
      MethodNode methodNode,
      FramesProvider framesProvider
  ) {
    this.classWrapper = classWrapper;
    this.methodNode = methodNode;
    this.framesProvider = framesProvider;
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
  @Unmodifiable
  public synchronized Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames() {
    if (this.frames == null) {
      // Lazy initialization
      this.frames = this.framesProvider.compute(this.classWrapper.classNode(), this.methodNode);
    }
    return frames;
  }

  public synchronized Map<AbstractInsnNode, Set<AbstractInsnNode>> getConsumersMap() {
    if (consumersMap == null) {
      // Lazy initialization
      this.consumersMap = MethodHelper.computeConsumersMap(this.frames);
    }
    return consumersMap;
  }

  /**
   * Creates new {@link InsnContext} instance at specified instruction
   *
   * @param insn instruction
   * @return new {@link InsnContext} instance
   */
  public InsnContext at(AbstractInsnNode insn) {
    return new InsnContext(insn, this);
  }

  public static MethodContext of(ClassWrapper classWrapper, MethodNode methodNode) {
    return of(classWrapper, methodNode, MethodHelper::analyzeSource);
  }

  /**
   * Creates new {@link MethodContext} instance
   */
  public static MethodContext of(ClassWrapper classWrapper, MethodNode methodNode, FramesProvider framesProvider) {
    return new MethodContext(classWrapper, methodNode, framesProvider);
  }
}
