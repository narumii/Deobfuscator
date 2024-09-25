package uwu.narumi.deobfuscator.api.asm.matcher.impl;

import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

public class StringMatch extends Match {

  private final String string;

  private StringMatch(String string) {
    this.string = string;
  }

  public static StringMatch of(String string) {
    return new StringMatch(string);
  }

  public static Match of() {
    return Match.of(context -> context.insn().isString());
  }

  @Override
  protected boolean test(MatchContext context) {
    return context.insn().isString() && context.insn().asString().equals(string);
  }
}
