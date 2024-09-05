package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.MatchContext;

public class SkipMatch extends Match {

  private SkipMatch() {}

  public static SkipMatch create() {
    return new SkipMatch();
  }

  @Override
  protected boolean test(MatchContext context) {
    return true;
  }
}
