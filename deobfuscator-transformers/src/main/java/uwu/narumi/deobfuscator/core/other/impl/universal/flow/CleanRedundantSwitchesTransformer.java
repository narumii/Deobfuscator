package uwu.narumi.deobfuscator.core.other.impl.universal.flow;

import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.AsmMathHelper;
import uwu.narumi.deobfuscator.api.helper.FramedInstructionsStream;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Optional;

/**
 * Clean redundant LOOKUPSWITCH and TABLESWITCH instructions
 */
public class CleanRedundantSwitchesTransformer extends Transformer {

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    FramedInstructionsStream.of(scope, context).forEach(insnContext -> {
      if (insnContext.insn().getOpcode() == LOOKUPSWITCH) {
        LookupSwitchInsnNode lookupSwitchInsn = (LookupSwitchInsnNode) insnContext.insn();

        Optional<LabelNode> optPredictedJump = AsmMathHelper.predictLookupSwitch(lookupSwitchInsn, insnContext.frame());
        if (optPredictedJump.isEmpty()) return;

        LabelNode predictedJump = optPredictedJump.get();
        // Remove value from stack
        insnContext.pop(1);
        // Replace lookup switch with predicted jump
        insnContext.methodNode().instructions.set(lookupSwitchInsn, new JumpInsnNode(GOTO, predictedJump));

        markChange();
      } else if (insnContext.insn().getOpcode() == TABLESWITCH) {
        TableSwitchInsnNode tableSwitchInsn = (TableSwitchInsnNode) insnContext.insn();

        Optional<LabelNode> optPredictedJump = AsmMathHelper.predictTableSwitch(tableSwitchInsn, insnContext.frame());
        if (optPredictedJump.isEmpty()) return;

        LabelNode predictedJump = optPredictedJump.get();
        // Remove value from stack
        insnContext.pop(1);
        // Replace lookup switch with predicted jump
        insnContext.methodNode().instructions.set(tableSwitchInsn, new JumpInsnNode(GOTO, predictedJump));

        markChange();
      }
    });
  }
}
