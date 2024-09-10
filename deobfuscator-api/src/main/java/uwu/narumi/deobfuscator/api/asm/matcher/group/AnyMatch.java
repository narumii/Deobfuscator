package uwu.narumi.deobfuscator.api.asm.matcher.group;

import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

public class AnyMatch extends Match {

  private final Match[] matches;

  private AnyMatch(Match[] matches) {
    this.matches = matches;
  }

  public static AnyMatch of(Match... matches) {
    return new AnyMatch(matches);
  }

  @Override
  protected boolean test(MatchContext context) {
    for (Match match : matches) {
      if (match.matchAndMerge(context.insnContext(), context)) return true;
    }

    return false;
  }
}
