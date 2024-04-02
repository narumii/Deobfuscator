package uwu.narumi.deobfuscator.api.asm.matcher.rule.group;

import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class MultiMatch implements Match {

  private final Match[] matches;

  private MultiMatch(Match[] matches) {
    this.matches = matches;
  }

  public static MultiMatch of(Match... matches) {
    return new MultiMatch(matches);
  }

  @Override
  public boolean test(AbstractInsnNode node) {
    for (Match match : matches) {
      if (!match.test(node)) return false;
    }

    return true;
  }
}
