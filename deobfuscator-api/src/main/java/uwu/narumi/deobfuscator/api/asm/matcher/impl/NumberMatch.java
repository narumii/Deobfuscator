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
    return predicate(ctx -> ctx.insn().isNumber());
  }

  public static Match numDouble() {
    return predicate(ctx -> ctx.insn().isDouble());
  }

  public static Match numFloat() {
    return predicate(ctx -> ctx.insn().isFloat());
  }

  public static Match numInteger() {
    return predicate(ctx -> ctx.insn().isInteger());
  }

  public static Match numLong() {
    return predicate(ctx -> ctx.insn().isLong());
  }

  @Override
  protected boolean test(MatchContext context) {
    return context.insn().isNumber() && context.insn().asNumber().equals(this.number);
  }
}
