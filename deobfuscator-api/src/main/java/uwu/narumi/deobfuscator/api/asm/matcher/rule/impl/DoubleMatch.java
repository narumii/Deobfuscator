package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.MatchContext;

public class DoubleMatch extends Match {

  private final double number;

  public DoubleMatch(double number) {
    this.number = number;
  }

  @Override
  protected boolean test(MatchContext context) {
    return context.insn().isDouble() && context.insn().asDouble() == number;
  }

  public static DoubleMatch of(double number) {
    return new DoubleMatch(number);
  }

  public static Match of() {
    return Match.predicate(context -> context.insn().isDouble());
  }
}
