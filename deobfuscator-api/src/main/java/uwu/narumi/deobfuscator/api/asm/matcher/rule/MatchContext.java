package uwu.narumi.deobfuscator.api.asm.matcher.rule;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.InstructionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable match context. After matching process, the context is frozen by {@link MatchContext#freeze()}
 *
 * @param insnContext Instruction context
 * @param storage Storage for saving some instructions in matching process. id -> match context
 * @param collectedInsns Collected instructions that matches this match and children matches
 */
public record MatchContext(
    InstructionContext insnContext,
    Map<String, MatchContext> storage,
    List<AbstractInsnNode> collectedInsns
) {
  public static MatchContext of(InstructionContext insnContext) {
    return new MatchContext(insnContext, new HashMap<>(), new ArrayList<>());
  }

  public MatchContext freeze() {
    return new MatchContext(this.insnContext, Collections.unmodifiableMap(this.storage), Collections.unmodifiableList(this.collectedInsns));
  }

  /**
   * Merges other {@link MatchContext} into this {@link MatchContext}
   */
  public void merge(MatchContext other) {
    this.storage.putAll(other.storage);
    this.collectedInsns.addAll(other.collectedInsns);
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

  /**
   * Removes all collected instructions
   */
  public void removeAll() {
    for (AbstractInsnNode insn : this.collectedInsns) {
      this.insnContext.methodContext().methodNode().instructions.remove(insn);
    }
  }
}
