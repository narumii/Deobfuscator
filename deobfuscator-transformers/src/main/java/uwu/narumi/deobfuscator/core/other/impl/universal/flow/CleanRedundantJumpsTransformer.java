package uwu.narumi.deobfuscator.core.other.impl.universal.flow;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.helper.AsmMathHelper;
import uwu.narumi.deobfuscator.api.transformer.FramedInstructionsTransformer;

import java.util.Optional;

public class CleanRedundantJumpsTransformer extends FramedInstructionsTransformer {
  @Override
  protected boolean transformInstruction(Context context, ClassWrapper classWrapper, MethodNode methodNode, AbstractInsnNode insn, Frame<OriginalSourceValue> frame) {
    if (!(insn instanceof JumpInsnNode jumpInsn)) return false;

    Optional<Boolean> optIfResult = AsmMathHelper.predictIf(jumpInsn, frame);

    if (optIfResult.isEmpty()) return false;

    boolean ifResult = optIfResult.get();

    if (AsmMathHelper.isOneValueCondition(insn.getOpcode())) {
      AsmHelper.removeValuesFromStack(methodNode, frame, 1);
    } else if (AsmMathHelper.isTwoValuesCondition(insn.getOpcode())) {
      AsmHelper.removeValuesFromStack(methodNode, frame, 2);
    }

    // Replace if with corresponding GOTO or remove it
    processRedundantIfStatement(methodNode, jumpInsn, ifResult);

    return true;
  }

  private void processRedundantIfStatement(MethodNode methodNode, JumpInsnNode ifStatement, boolean ifResult) {
    if (!ifResult) {
      // Remove unreachable if statement
      methodNode.instructions.remove(ifStatement);
    } else {
      // Replace always reachable if statement with GOTO
      ifStatement.setOpcode(GOTO);
    }
  }
}
