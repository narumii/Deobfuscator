package uwu.narumi.deobfuscator.api.asm.matcher.group;

import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

public class NotMatch extends Match {

  private final Match match;

  private NotMatch(Match match) {
    this.match = match;
  }

  public static NotMatch of(Match match) {
    return new NotMatch(match);
  }

  @Override
  protected boolean test(MatchContext context) {
    return !match.matchAndMerge(context.insnContext(), context);
  }
}
