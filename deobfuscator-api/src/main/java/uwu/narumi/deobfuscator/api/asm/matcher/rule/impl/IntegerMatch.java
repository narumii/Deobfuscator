package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.MatchContext;

public class IntegerMatch extends Match {

  private final int number;

  public IntegerMatch(int number) {
    this.number = number;
  }

  public static IntegerMatch of(int number) {
    return new IntegerMatch(number);
  }

  public static Match of() {
    return Match.predicate(context -> context.insn().isInteger());
  }

  @Override
  protected boolean test(MatchContext context) {
    return context.insn().isInteger() && context.insn().asInteger() == number;
  }
}
