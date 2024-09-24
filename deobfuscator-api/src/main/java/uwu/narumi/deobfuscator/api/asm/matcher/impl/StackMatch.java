package uwu.narumi.deobfuscator.api.asm.matcher.impl;

import org.objectweb.asm.NamedOpcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

/**
 * Match instruction from stack
 */
public class StackMatch extends Match {
  private final int stackValueIdx;
  private final Match match;
  private final boolean originalValue;

  /**
   * @param stackValueIdx Index of the value in the stack, starting from the top of the stack, so '0' is the top value.
   * @param match A {@link Match} to match against that stack value
   */
  private StackMatch(int stackValueIdx, Match match, boolean originalValue) {
    this.stackValueIdx = stackValueIdx;
    this.match = match;
    this.originalValue = originalValue;
  }

  public static StackMatch of(int stackValueIdx, Match match) {
    return new StackMatch(stackValueIdx, match, false);
  }

  public static StackMatch ofOriginal(int stackValueIdx, Match match) {
    return new StackMatch(stackValueIdx, match, true);
  }

  @Override
  protected boolean test(MatchContext context) {
    if (context.insnContext().methodContext().frames() == null) {
      throw new IllegalStateException("Got frameless method context");
    }

    if (context.frame() == null) {
      // If we expect stack values, then frame can't be null
      return false;
    }

    // Pop values from stack and match them
    int index = context.frame().getStackSize() - (this.stackValueIdx + 1);
    if (index < 0) {
      // If the stack value should exist but does not, then it does not match
      return false;
    }

    if (this.match instanceof SkipMatch) {
      // Skip match earlier
      return true;
    }

    Frame<OriginalSourceValue> frame = context.frame();
    OriginalSourceValue sourceValue = frame.getStack(index);
    if (this.originalValue) {
      sourceValue = sourceValue.originalSource;
    }

    if (!sourceValue.isOneWayProduced()) {
      // We only want stack values that are one way produced
      return false;
    }

    AbstractInsnNode stackValueInsn = sourceValue.getProducer();
    return this.match.matchAndMerge(context.insnContext().of(stackValueInsn), context);
  }
}
