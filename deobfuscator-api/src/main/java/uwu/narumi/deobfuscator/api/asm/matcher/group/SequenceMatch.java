package uwu.narumi.deobfuscator.api.asm.matcher.group;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import uwu.narumi.deobfuscator.api.asm.InsnContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches instructions in sequence
 */
// TODO: backwards match?
public class SequenceMatch extends Match {

  private static final Match FRAME_MATCH = Match.of(context -> context.insn() instanceof FrameNode);

  private final Match[] matches;
  private final List<Match> skipMatches = new ArrayList<>(List.of(FRAME_MATCH));

  private SequenceMatch(Match[] matches) {
    this.matches = matches;
  }

  public static SequenceMatch of(Match... matches) {
    return new SequenceMatch(matches);
  }

  /**
   * You can specify which instructions should be skipped during walking through instructions
   */
  public SequenceMatch skip(Match... matches) {
    this.skipMatches.addAll(List.of(matches));
    return this;
  }

  public SequenceMatch doNotSkipFrames() {
    this.skipMatches.remove(FRAME_MATCH);
    return this;
  }

  @Override
  protected boolean test(MatchContext context) {
    AbstractInsnNode currentInsn = context.insn();
    int matchIdx = 0;

    while (matchIdx < matches.length) {
      if (currentInsn == null) {
        // Expected instruction but no instructions left
        return false;
      }

      InsnContext currentInsnContext = context.insnContext().of(currentInsn);
      if (this.skipMatches.stream().anyMatch(match -> match.matches(currentInsnContext))) {
        // Skip instruction
        currentInsn = currentInsn.getNext();
        continue;
      }

      // Find match
      Match match = this.matches[matchIdx];
      if (!match.matchAndMerge(currentInsnContext, context)) {
        // No match
        return false;
      }

      // Go to next instruction
      currentInsn = currentInsn.getNext();
      matchIdx++;
    }

    return true;
  }
}
