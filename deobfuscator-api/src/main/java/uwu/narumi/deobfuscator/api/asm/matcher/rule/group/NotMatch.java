package uwu.narumi.deobfuscator.api.asm.matcher.rule.group;

import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class NotMatch implements Match {

  private final Match match;

  private NotMatch(Match match) {
    this.match = match;
  }

  public static NotMatch of(Match match) {
    return new NotMatch(match);
  }

  @Override
  public boolean test(AbstractInsnNode node) {
    return !match.test(node);
  }
}
