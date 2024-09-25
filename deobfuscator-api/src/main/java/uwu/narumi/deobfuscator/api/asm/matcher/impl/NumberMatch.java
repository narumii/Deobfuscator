package uwu.narumi.deobfuscator.api.asm.matcher.impl;

import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

public class NumberMatch extends Match {
  private final Number number;

  private NumberMatch(Number number) {
    this.number = number;
  }

  public static NumberMatch of(Number number) {
    return new NumberMatch(number);
  }

  public static Match of() {
    return Match.of(ctx -> ctx.insn().isNumber());
  }

  public static Match numDouble() {
    return Match.of(ctx -> ctx.insn().isDouble());
  }

  public static Match numFloat() {
    return Match.of(ctx -> ctx.insn().isFloat());
  }

  public static Match numInteger() {
    return Match.of(ctx -> ctx.insn().isInteger());
  }

  public static Match numLong() {
    return Match.of(ctx -> ctx.insn().isLong());
  }

  @Override
  protected boolean test(MatchContext context) {
    return context.insn().isNumber() && context.insn().asNumber().equals(this.number);
  }
}
