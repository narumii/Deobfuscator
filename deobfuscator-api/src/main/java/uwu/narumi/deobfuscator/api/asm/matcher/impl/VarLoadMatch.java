package uwu.narumi.deobfuscator.api.asm.matcher.impl;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.VarInsnNode;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

public class VarLoadMatch extends Match {
  private Match localStoreMatch = null;

  private VarLoadMatch() {
  }

  public static VarLoadMatch of() {
    return new VarLoadMatch();
  }

  /**
   * Match local variable store instruction of this variable
   */
  public VarLoadMatch localStoreMatch(@Nullable Match match) {
    this.localStoreMatch = match;
    return this;
  }

  @Override
  protected boolean test(MatchContext context) {
    boolean matches = context.insnContext().insn().isVarLoad();

    // Match local variable store instruction
    if (matches && localStoreMatch != null) {
      VarInsnNode varInsn = (VarInsnNode) context.insnContext().insn();

      matches = FrameMatch.localVariable(varInsn.var, localStoreMatch).matchAndMerge(context.insnContext(), context);
    }

    return matches;
  }
}
