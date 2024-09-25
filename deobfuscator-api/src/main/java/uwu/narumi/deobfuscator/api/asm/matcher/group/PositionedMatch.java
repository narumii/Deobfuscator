package uwu.narumi.deobfuscator.api.asm.matcher.group;

import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

public class PositionedMatch extends Match {

  private final int offset;
  private final boolean previous;
  private final boolean skipAsmInstructions;
  private final Match match;

  private PositionedMatch(int offset, boolean skipAsmInstructions, Match match) {
    this.offset = Math.abs(offset);
    this.previous = offset < 0;
    this.skipAsmInstructions = skipAsmInstructions;
    this.match = match;
  }

  @Override
  protected boolean test(MatchContext context) {
    return this.match.matchAndMerge(context.insnContext().of(walk(context.insn())), context);
  }

  private AbstractInsnNode walk(AbstractInsnNode node) {
    if (previous) {
      node = skipAsmInstructions ? node.previous(offset) : node.getPrevious(offset);
    } else {
      node = skipAsmInstructions ? node.next(offset) : node.getNext(offset);
    }

    return node;
  }
}
