package uwu.narumi.deobfuscator.api.asm.matcher.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

/**
 * Match instruction from stack
 */
public class FrameMatch extends Match {
  private final Match match;
  private final FrameMatchOptions options;

  /**
   * @param match A {@link Match} to match against that stack value
   * @param options Options for the frame match
   */
  private FrameMatch(Match match, FrameMatchOptions options) {
    this.match = match;
    this.options = options;
  }

  public static FrameMatch stack(int stackValueIdx, Match match) {
    return new FrameMatch(match, new StackFrameOptions(stackValueIdx, false));
  }

  public static FrameMatch stackOriginal(int stackValueIdx, Match match) {
    return new FrameMatch(match, new StackFrameOptions(stackValueIdx, true));
  }

  public static FrameMatch localVariable(int localVariableIdx, Match match) {
    return new FrameMatch(match, new LocalVariableFrameOptions(localVariableIdx));
  }

  @Override
  protected boolean test(MatchContext context) {
    if (context.frame() == null) {
      // If we expect stack values, then frame can't be null
      return false;
    }

    // Get the source value
    OriginalSourceValue sourceValue;
    if (this.options instanceof StackFrameOptions stackFrameOptions) {
      // Pop values from stack and match them
      int index = context.frame().getStackSize() - (stackFrameOptions.stackValueIdx + 1);
      if (index < 0) {
        // If the stack value should exist but does not, then it does not match
        return false;
      }

      Frame<OriginalSourceValue> frame = context.frame();
      sourceValue = frame.getStack(index);
      if (stackFrameOptions.originalValue) {
        sourceValue = sourceValue.originalSource;
      }
    } else if (this.options instanceof LocalVariableFrameOptions lvFrameOptions) {
      Frame<OriginalSourceValue> frame = context.frame();
      sourceValue = frame.getLocal(lvFrameOptions.localVariableIdx);
    } else {
      throw new IllegalStateException("Unknown frame match options");
    }

    if (!sourceValue.isOneWayProduced()) {
      // We only want stack values that are one way produced
      return false;
    }

    AbstractInsnNode stackValueInsn = sourceValue.getProducer();
    return this.match.matchAndMerge(context.insnContext().of(stackValueInsn), context);
  }

  sealed interface FrameMatchOptions permits StackFrameOptions, LocalVariableFrameOptions {
  }

  /**
   * @param stackValueIdx Index of the value in the stack, starting from the top of the stack, so '0' is the top value.
   * @param originalValue
   */
  record StackFrameOptions(int stackValueIdx, boolean originalValue) implements FrameMatchOptions {
  }

  record LocalVariableFrameOptions(int localVariableIdx) implements FrameMatchOptions {
  }
}
