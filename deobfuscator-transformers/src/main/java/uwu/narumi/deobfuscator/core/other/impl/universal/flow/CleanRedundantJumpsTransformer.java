package uwu.narumi.deobfuscator.core.other.impl.universal.flow;

import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.api.helper.AsmMathHelper;
import uwu.narumi.deobfuscator.api.helper.FramedInstructionsStream;
import uwu.narumi.deobfuscator.api.helper.MethodHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Optional;

public class CleanRedundantJumpsTransformer extends Transformer {
  @Override
  protected void transform() throws Exception {
    FramedInstructionsStream.of(this)
        .framesProvider(MethodHelper::analyzeSourcePredictJumps) // Predict jumps
        .forEach(insnContext -> {
          if (!(insnContext.insn() instanceof JumpInsnNode jumpInsn)) return;

          Optional<Boolean> optIfResult = AsmMathHelper.predictIf(jumpInsn, insnContext.frame());

          if (optIfResult.isEmpty()) return;

          boolean ifResult = optIfResult.get();

          if (AsmMathHelper.isOneValueCondition(jumpInsn.getOpcode()) || AsmMathHelper.isTwoValuesCondition(jumpInsn.getOpcode())) {
            insnContext.placePops();
          }

          // Replace if with corresponding GOTO or remove it
          processRedundantIfStatement(insnContext.methodNode(), jumpInsn, ifResult);

          markChange();
        });
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
