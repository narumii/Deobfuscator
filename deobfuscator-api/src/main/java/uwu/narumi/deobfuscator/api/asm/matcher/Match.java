package uwu.narumi.deobfuscator.api.asm.matcher;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.api.asm.InsnContext;
import uwu.narumi.deobfuscator.api.asm.MethodContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A class that matches instructions providing easy api to interact with.
 */
public abstract class Match {

  private Transformation transformation;
  /**
   * @see #capture(String)
   */
  private String captureId = null;

  /**
   * Tests given instruction if it matches current {@link Match}
   *
   * @param insnContext Instruction context
   * @return If matches
   */
  public boolean matches(InsnContext insnContext) {
    return this.matchResult(insnContext) != null;
  }

  /**
   * Matches the instruction and merges if successful
   *
   * @param insnContext         Instruction context
   * @param currentMatchContext Match context to merge into
   * @return If matches
   */
  @ApiStatus.Internal
  public boolean matchAndMerge(InsnContext insnContext, MatchContext currentMatchContext) {
    MatchContext result = this.matchResult(insnContext);
    if (result != null) {
      currentMatchContext.merge(result);
    }
    return result != null;
  }

  /**
   * Finds all matches in the method
   *
   * @param methodContext Method context
   * @return List of all matches
   */
  public List<MatchContext> findAllMatches(MethodContext methodContext) {
    List<MatchContext> allMatches = new ArrayList<>();

    for (AbstractInsnNode insn : methodContext.methodNode().instructions) {
      InsnContext insnContext = methodContext.at(insn);
      MatchContext match = this.matchResult(insnContext);
      if (match != null) {
        allMatches.add(match);
      }
    }

    return allMatches;
  }

  @Nullable
  public MatchContext findFirstMatch(MethodContext methodContext) {
    return this.findAllMatches(methodContext).stream().findFirst().orElse(null);
  }

  /**
   * @return {@link MatchContext} if matches or {@code null} if it does not match
   */
  public MatchContext matchResult(InsnContext insnContext) {
    // Create MatchContext
    MatchContext context = MatchContext.of(insnContext);

    // Test against this match
    if (!this.test(context)) {
      // No match
      return null;
    }

    if (this.captureId != null) {
      // Capture this instruction
      context.captures().put(this.captureId, context);
    }

    if (!context.collectedInsns().contains(context.insn())) {
      context.collectedInsns().add(context.insn());
    }

    // We have match!
    return context.freeze();
  }

  /**
   * @see #matches(InsnContext)
   */
  protected abstract boolean test(MatchContext context);

  public Match and(Match match) {
    return Match.of(context -> this.matchAndMerge(context.insnContext(), context) && match.matchAndMerge(context.insnContext(), context));
  }

  public Match or(Match match) {
    return Match.of(context -> this.matchAndMerge(context.insnContext(), context) || match.matchAndMerge(context.insnContext(), context));
  }

  public Match not() {
    return Match.of(context -> !matchAndMerge(context.insnContext(), context));
  }

  public Match defineTransformation(Transformation transformation) {
    this.transformation = transformation;
    return this;
  }

  /**
   * If matches, then captures instruction to {@link MatchContext#captures()} for further processing
   *
   * @param id Under what id this instruction should be captured to {@link MatchContext#captures()}
   */
  public Match capture(String id) {
    this.captureId = id;
    return this;
  }

  public Transformation transformation() {
    return this.transformation;
  }

  /**
   * Create {@link Match} from lambda
   *
   * @param predicate Your lambda predicate
   * @return A new {@link Match}
   */
  public static Match of(Predicate<MatchContext> predicate) {
    return new Match() {
      @Override
      protected boolean test(MatchContext context) {
        return predicate.test(context);
      }
    };
  }

  @FunctionalInterface
  public interface Transformation {
    /**
     * Executes given action
     *
     * @param context Current instruction context
     * @return If changed
     */
    boolean transform(InsnContext context);
  }
}
