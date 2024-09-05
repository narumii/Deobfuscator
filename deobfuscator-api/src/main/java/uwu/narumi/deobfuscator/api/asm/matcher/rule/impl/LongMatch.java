package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.MatchContext;

public class LongMatch extends Match {

  private final long number;

  public LongMatch(long number) {
    this.number = number;
  }

  public static LongMatch of(long number) {
    return new LongMatch(number);
  }

  public static Match of() {
    return Match.predicate(context -> context.insn().isLong());
  }

  @Override
  protected boolean test(MatchContext context) {
    return context.insn().isLong() && context.insn().asLong() == number;
  }
}
