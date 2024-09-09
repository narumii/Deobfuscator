package uwu.narumi.deobfuscator.api.asm.matcher.impl;

import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

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
