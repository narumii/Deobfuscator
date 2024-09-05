package uwu.narumi.deobfuscator.api.asm.matcher.rule;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.InstructionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Match context
 *
 * @param insnContext Instruction context
 * @param storage Storage for saving some instructions in matching process. id -> instruction
 * @param collectedInsns Collected instructions that matches this match and children matches
 */
public record MatchContext(
    InstructionContext insnContext,
    Map<String, AbstractInsnNode> storage,
    List<AbstractInsnNode> collectedInsns
) {
  public static MatchContext of(InstructionContext insnContext) {
    return new MatchContext(insnContext, new HashMap<>(), new ArrayList<>());
  }

  public MatchContext ofInsn(AbstractInsnNode insn) {
    return new MatchContext(insnContext.ofInsn(insn), storage, collectedInsns);
  }

  /**
   * @see InstructionContext#insn()
   */
  public AbstractInsnNode insn() {
    return this.insnContext.insn();
  }

  /**
   * @see InstructionContext#frame()
   */
  public Frame<OriginalSourceValue> frame() {
    return this.insnContext.frame();
  }
}
