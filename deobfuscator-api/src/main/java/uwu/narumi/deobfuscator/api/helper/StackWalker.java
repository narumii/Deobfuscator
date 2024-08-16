package uwu.narumi.deobfuscator.api.helper;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A helper class to walk through the stack and get the only one possible and real producer of a {@link SourceValue}.
 * See {@link #getOnlyOnePossibleProducer(Map, SourceValue)} for more details.
 *
 * @author EpicPlayerA10
 */
public class StackWalker implements Opcodes {
  /**
   * Get the only one possible producer of a {@link SourceValue} that is passed to a method.
   * The main advantage of this method is that it also follows DUP instructions and variable
   * references to get to the real producer that will be actually useful in another transformers.
   *
   * <p>
   * Consider this example of instructions:
   * <pre>
   * 1: A:
   * 2:   ICONST_1
   * 3:   ISTORE exVar
   * 4: B:
   * 5:   ILOAD exVar
   * 6:   DUP
   * 7:   IFNE C
   * 8: C:
   * 9:   ...
   * </pre>
   * When this function is called with instruction in line 6, then it will follow all
   * instructions (DUP, ILOAD) to get real producer of this instruction in line 6. In this
   * example, it will return instruction in line 2.
   *
   * @param sourceValue The value to get the producer of.
   * @param frames      The frames required to follow DUPs instructions
   * @return The producer of the value, or an empty optional if there are multiple producers.
   */
  public static Optional<StackValue> getOnlyOnePossibleProducer(Map<AbstractInsnNode, Frame<SourceValue>> frames, SourceValue sourceValue) {
    Set<AbstractInsnNode> valueToStoreInsns = sourceValue.insns;
    if (valueToStoreInsns.size() != 1) return Optional.empty();

    AbstractInsnNode insn = valueToStoreInsns.iterator().next();

    // Follow DUP instructions
    StackValue stackValue = buildStackValue(frames, insn);
    if (stackValue.possibleValues().size() != 1) return Optional.empty();

    return Optional.of(stackValue);
  }

  public static StackValue buildStackValue(Map<AbstractInsnNode, Frame<SourceValue>> frames, AbstractInsnNode startInsn) {
    return buildStackValue(new StackValue.BuilderContext(frames, startInsn));
  }

  /**
   * Follows DUP and vars (ILOAD instructions).
   */
  private static StackValue buildStackValue(StackValue.BuilderContext builderContext) {
    if (builderContext.currentInsn().getOpcode() == DUP) {
      // Follow DUP instructions
      return followDupInstructions(builderContext);
    } else if (builderContext.currentInsn().getOpcode() == ILOAD) {
      // Follow vars references
      Optional<StackValue> stackValue = followVars(builderContext);
      if (stackValue.isPresent()) return stackValue.get();
    }

    return builderContext.build(
        Set.of(builderContext.currentInsn())
    );
  }

  /**
   * Follows DUP instructions.
   */
  private static StackValue followDupInstructions(StackValue.BuilderContext builderContext) {
    Frame<SourceValue> frame = builderContext.getCurrentFrame();

    // Follow DUP instructions
    Set<AbstractInsnNode> insns = frame.getStack(frame.getStackSize() - 1).insns;

    // If the DUP instruction is followed by another DUP instruction, follow it
    if (insns.size() == 1) {
      AbstractInsnNode firstInsn = insns.iterator().next();

      // We can walk further
      return buildStackValue(
          builderContext.currentInsn(firstInsn)
      );
    }

    // We have the final stack value! Return it
    return builderContext.build(insns);
  }

  /**
   * Follows vars references (ILOAD instructions).
   */
  private static Optional<StackValue> followVars(StackValue.BuilderContext builderContext) {
    Frame<SourceValue> currentFrame = builderContext.getCurrentFrame();

    // Follow vars (ILOAD instructions)
    VarInsnNode varInsn = (VarInsnNode) builderContext.currentInsn();

    // Get ISTORE of this variable
    Optional<StackValue> storeVariableInsn = getOnlyOnePossibleProducer(builderContext.frames(), currentFrame.getLocal(varInsn.var));
    if (storeVariableInsn.isEmpty()) return Optional.empty();

    // Get value that is passed to ISTORE
    Frame<SourceValue> storeVarFrame = builderContext.frames().get(storeVariableInsn.get().value());
    if (storeVarFrame == null) return Optional.empty();

    if (builderContext.variableInit().isPresent()) {
      throw new IllegalStateException("Multiple ISTORE? They should be reduced by StackWalker#getOnlyOnePossibleProducer");
    }

    builderContext = builderContext
        .variableInit(storeVariableInsn);

    SourceValue sourceValue = storeVarFrame.getStack(storeVarFrame.getStackSize() - 1);
    Optional<StackValue> valueInVarInsn = getOnlyOnePossibleProducer(builderContext.frames(), sourceValue);

    // If var has multiple values, we can't follow it. Return immediately
    if (valueInVarInsn.isEmpty()) {
      return Optional.of(builderContext.build(sourceValue.insns));
    }

    // We can walk further
    return Optional.of(buildStackValue(
        builderContext
            .currentInsn(valueInVarInsn.get().value())
    ));
  }

  /**
   * @param possibleValues All possible values from walking on DUP instructions.
   *                       Don't use it to remove stack value from instructions list.
   *
   * @param startInsn      The starting instruction that was passed to the method.
   *                       Used to remove reference to this stack value
   *
   * @param variableInit   If StackValue was obtained from var then it holds the
   *                       ISTORE instruction that stores the value of this stack value.
   */
  public record StackValue(Set<AbstractInsnNode> possibleValues, AbstractInsnNode startInsn,
                           Optional<StackValue> variableInit) {
    public boolean isVariable() {
      return variableInit.isPresent();
    }

    public AbstractInsnNode value() {
      return possibleValues.iterator().next();
    }

    /**
     * Remove the stack value from the instructions list. Doesn't remove the variable associated with it.
     *
     * @param methodNode The method node to remove the stack value from.
     */
    public void remove(MethodNode methodNode) {
      methodNode.instructions.remove(this.startInsn);
    }

    /**
     * Remove the stack value from the instructions list along with its variable init.
     *
     * @param methodNode The method node to remove the stack value from.
     * @param toRemove   The list to add the instructions to remove variable init.
     */
    public void removeWithVarInit(MethodNode methodNode, List<AbstractInsnNode> toRemove) {
      this.remove(methodNode);

      if (this.isVariable()) {
        // ISTORE
        if (!toRemove.contains(this.variableInit.get().startInsn())) {
          toRemove.add(this.variableInit.get().startInsn());
        }
        // Stored value
        if (!toRemove.contains(this.value())) {
          toRemove.add(this.value());
        }
      }
    }

    public static class BuilderContext {
      // Unmodifiable
      private final Map<AbstractInsnNode, Frame<SourceValue>> frames;

      private final AbstractInsnNode startInsn;
      private final AbstractInsnNode currentInsn;
      private final Optional<StackValue> variableInit;

      public BuilderContext(Map<AbstractInsnNode, Frame<SourceValue>> frames, AbstractInsnNode startInsn) {
        this(frames, startInsn, startInsn, Optional.empty());
      }

      public BuilderContext(
          Map<AbstractInsnNode, Frame<SourceValue>> frames,
          AbstractInsnNode startInsn,
          AbstractInsnNode currentInsn,
          Optional<StackValue> variableInit
      ) {
        this.frames = frames;

        this.startInsn = startInsn;
        this.currentInsn = currentInsn;
        this.variableInit = variableInit;
      }

      public Frame<SourceValue> getCurrentFrame() {
        return frames.get(currentInsn);
      }

      public Map<AbstractInsnNode, Frame<SourceValue>> frames() {
        return frames;
      }

      public AbstractInsnNode startInsn() {
        return startInsn;
      }

      public BuilderContext startInsn(AbstractInsnNode startInsn) {
        return new BuilderContext(this.frames, startInsn, this.currentInsn, this.variableInit);
      }

      public AbstractInsnNode currentInsn() {
        return currentInsn;
      }

      public BuilderContext currentInsn(AbstractInsnNode currentInsn) {
        return new BuilderContext(this.frames, this.startInsn, currentInsn, this.variableInit);
      }

      public Optional<StackValue> variableInit() {
        return variableInit;
      }

      public BuilderContext variableInit(Optional<StackValue> variableInit) {
        return new BuilderContext(this.frames, this.startInsn, this.currentInsn, variableInit);
      }

      /**
       * Build the {@link StackValue} with the current context and with given possible values.
       *
       * @param possibleValues All possible values of the {@link StackValue}
       * @return Fresh {@link StackValue} instance.
       */
      public StackValue build(Set<AbstractInsnNode> possibleValues) {
        return new StackValue(possibleValues, this.startInsn, this.variableInit);
      }
    }
  }
}
