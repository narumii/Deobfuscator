package uwu.narumi.deobfuscator.api.asm.matcher;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.InsnContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Immutable match context. After matching process, the context is frozen by {@link MatchContext#freeze()}
 */
public class MatchContext {
  private final InsnContext insnContext;
  private final Map<String, MatchContext> captures;
  private final Set<AbstractInsnNode> collectedInsns;

  private MatchContext(InsnContext insnContext, Map<String, MatchContext> captures, Set<AbstractInsnNode> collectedInsns) {
    this.insnContext = insnContext;
    this.captures = captures;
    this.collectedInsns = collectedInsns;
  }

  public static MatchContext of(InsnContext insnContext) {
    return new MatchContext(insnContext, new HashMap<>(), new HashSet<>());
  }

  public MatchContext freeze() {
    return new MatchContext(this.insnContext, Collections.unmodifiableMap(this.captures), Collections.unmodifiableSet(this.collectedInsns));
  }

  /**
   * Merges other {@link MatchContext} into this {@link MatchContext}.
   *
   * @see Match#matchAndMerge(InsnContext, MatchContext)
   */
  void merge(MatchContext other) {
    this.captures.putAll(other.captures);
    this.collectedInsns.addAll(other.collectedInsns);
  }

  /**
   * @see InsnContext#insn()
   */
  public AbstractInsnNode insn() {
    return this.insnContext.insn();
  }

  /**
   * @see InsnContext#frame()
   */
  public Frame<OriginalSourceValue> frame() {
    return this.insnContext.frame();
  }

  /**
   * Instruction context
   */
  public InsnContext insnContext() {
    return insnContext;
  }

  /**
   * Captured instructions in a matching process. id -> match context
   *
   * @see Match#capture(String)
   */
  public Map<String, MatchContext> captures() {
    return captures;
  }

  /**
   * Collected instructions that matches this match and children matches
   */
  public Set<AbstractInsnNode> collectedInsns() {
    return collectedInsns;
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
