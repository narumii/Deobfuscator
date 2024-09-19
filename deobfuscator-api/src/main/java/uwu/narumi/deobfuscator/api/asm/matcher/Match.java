package uwu.narumi.deobfuscator.api.asm.matcher;

import uwu.narumi.deobfuscator.api.asm.InstructionContext;

import java.util.function.Predicate;

/**
 * A class that matches instructions providing easy api to interact with.
 */
public abstract class Match {

  private Transformation transformation;
  /**
   * @see #save(String)
   */
  private String saveId = null;

  /**
   * Tests given instruction if it matches current {@link Match}
   *
   * @param insnContext Instruction context
   * @return If matches
   */
  public boolean matches(InstructionContext insnContext) {
    return this.matchResult(insnContext) != null;
  }

  /**
   * Matches the instrustion and merges if successful
   *
   * @param insnContext         Instruction context
   * @param currentMatchContext Match context to merge into
   * @return If matches
   */
  public boolean matchAndMerge(InstructionContext insnContext, MatchContext currentMatchContext) {
    MatchContext result = this.matchResult(insnContext);
    if (result != null) {
      currentMatchContext.merge(result);
    }
    return result != null;
  }

  /**
   * @return {@link MatchContext} if matches or {@code null} if it does not match
   */
  public MatchContext matchResult(InstructionContext insnContext) {
    // Create MatchContext
    MatchContext context = MatchContext.of(insnContext);

    // Test against this match
    if (!this.test(context)) {
      // No match
      return null;
    }

    if (this.saveId != null) {
      // Save to storage
      context.storage().put(this.saveId, context);
    }

    if (!context.collectedInsns().contains(context.insn())) {
      context.collectedInsns().add(context.insn());
    }

    // We have match!
    return context.freeze();
  }

  /**
   * @see #matches(InstructionContext)
   */
  protected abstract boolean test(MatchContext context);

  public Match and(Match match) {
    return Match.predicate(context -> this.matchAndMerge(context.insnContext(), context) && match.matchAndMerge(context.insnContext(), context));
  }

  public Match or(Match match) {
    return Match.predicate(context -> this.matchAndMerge(context.insnContext(), context) || match.matchAndMerge(context.insnContext(), context));
  }

  public Match not() {
    return Match.predicate(context -> !matchAndMerge(context.insnContext(), context));
  }

  public Match defineTransformation(Transformation transformation) {
    this.transformation = transformation;
    return this;
  }

  /**
   * If matches, then saves instruction to {@link MatchContext#storage()} for further processing
   *
   * @param id Under what id this instruction should be saved in {@link MatchContext#storage()}
   */
  public Match save(String id) {
    this.saveId = id;
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
  public static Match predicate(Predicate<MatchContext> predicate) {
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
    boolean transform(InstructionContext context);
  }
}
