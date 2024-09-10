package uwu.narumi.deobfuscator.api.asm.matcher.group;

import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

public class AllMatch extends Match {

  private final Match[] matches;

  private AllMatch(Match[] matches) {
    this.matches = matches;
  }

  public static AllMatch of(Match... matches) {
    return new AllMatch(matches);
  }

  @Override
  protected boolean test(MatchContext context) {
    for (Match match : matches) {
      if (!match.matchAndMerge(context.insnContext(), context)) return false;
    }

    return true;
  }
}
