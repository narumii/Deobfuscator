package uwu.narumi.deobfuscator.api.asm.matcher.impl;

import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

public class FloatMatch extends Match {

  private final float number;

  public FloatMatch(float number) {
    this.number = number;
  }

  public static FloatMatch of(float number) {
    return new FloatMatch(number);
  }

  public static Match of() {
    return Match.predicate(context -> context.insn().isFloat());
  }

  @Override
  protected boolean test(MatchContext context) {
    return context.insn().isFloat() && context.insn().asFloat() == number;
  }
}
