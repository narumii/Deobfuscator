package uwu.narumi.deobfuscator.api.asm.matcher.rule.group;

import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;

public class OrMatch implements Match {

  private final Match[] matches;

  private OrMatch(Match[] matches) {
    this.matches = matches;
  }

  public static OrMatch of(Match... matches) {
    return new OrMatch(matches);
  }

  @Override
  public boolean test(AbstractInsnNode node) {
    for (Match match : matches) {
      if (match.test(node)) return true;
    }

    return false;
  }
}
