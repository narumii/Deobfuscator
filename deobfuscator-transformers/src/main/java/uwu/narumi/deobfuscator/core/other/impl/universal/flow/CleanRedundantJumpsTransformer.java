package uwu.narumi.deobfuscator.core.other.impl.universal.flow;

import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.api.asm.InstructionContext;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.AsmMathHelper;
import uwu.narumi.deobfuscator.api.transformer.FramedInstructionsTransformer;

import java.util.Optional;

public class CleanRedundantJumpsTransformer extends FramedInstructionsTransformer {
  @Override
  protected boolean transformInstruction(Context context, InstructionContext insnContext) {
    if (!(insnContext.insn() instanceof JumpInsnNode jumpInsn)) return false;

    Optional<Boolean> optIfResult = AsmMathHelper.predictIf(jumpInsn, insnContext.frame());

    if (optIfResult.isEmpty()) return false;

    boolean ifResult = optIfResult.get();

    if (AsmMathHelper.isOneValueCondition(jumpInsn.getOpcode())) {
      insnContext.pop(1);
    } else if (AsmMathHelper.isTwoValuesCondition(jumpInsn.getOpcode())) {
      insnContext.pop(2);
    }

    // Replace if with corresponding GOTO or remove it
    processRedundantIfStatement(insnContext.methodNode(), jumpInsn, ifResult);

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
