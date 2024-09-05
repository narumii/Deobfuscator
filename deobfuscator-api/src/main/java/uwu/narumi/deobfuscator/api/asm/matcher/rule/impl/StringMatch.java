package uwu.narumi.deobfuscator.api.asm.matcher.rule.impl;

import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.MatchContext;

public class StringMatch extends Match {

  private final String string;

  private StringMatch(String string) {
    this.string = string;
  }

  public static StringMatch of(String string) {
    return new StringMatch(string);
  }

  public static Match of() {
    return Match.predicate(context -> context.insn().isString());
  }

  @Override
  protected boolean test(MatchContext context) {
    return context.insn().isString() && context.insn().asString().equals(string);
  }
}
