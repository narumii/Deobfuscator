package uwu.narumi.deobfuscator.api.asm.matcher.rule.group;

import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.MatchContext;

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
      if (!match.matches(context)) return false;
    }

    return true;
  }
}
