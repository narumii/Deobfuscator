package uwu.narumi.deobfuscator.core.other.impl.universal.flow;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.helper.AsmMathHelper;
import uwu.narumi.deobfuscator.api.transformer.FramedInstructionsTransformer;

import java.util.Map;
import java.util.Optional;

/**
 * Clean redundant LOOKUPSWITCH and TABLESWITCH instructions
 */
public class CleanRedundantSwitchesTransformer extends FramedInstructionsTransformer {

  @Override
  protected boolean transformInstruction(Context context, ClassWrapper classWrapper, MethodNode methodNode, Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames, AbstractInsnNode insn, Frame<OriginalSourceValue> frame) {
    if (insn.getOpcode() == LOOKUPSWITCH) {
      LookupSwitchInsnNode lookupSwitchInsn = (LookupSwitchInsnNode) insn;

      Optional<LabelNode> optPredictedJump = AsmMathHelper.predictLookupSwitch(lookupSwitchInsn, frame);
      if (optPredictedJump.isEmpty()) return false;

      LabelNode predictedJump = optPredictedJump.get();
      // Remove value from stack
      AsmHelper.removeValuesFromStack(methodNode, frame, 1);
      // Replace lookup switch with predicted jump
      methodNode.instructions.set(lookupSwitchInsn, new JumpInsnNode(GOTO, predictedJump));
    } else if (insn.getOpcode() == TABLESWITCH) {
      TableSwitchInsnNode tableSwitchInsn = (TableSwitchInsnNode) insn;

      Optional<LabelNode> optPredictedJump = AsmMathHelper.predictTableSwitch(tableSwitchInsn, frame);
      if (optPredictedJump.isEmpty()) return false;

      LabelNode predictedJump = optPredictedJump.get();
      // Remove value from stack
      AsmHelper.removeValuesFromStack(methodNode, frame, 1);
      // Replace lookup switch with predicted jump
      methodNode.instructions.set(tableSwitchInsn, new JumpInsnNode(GOTO, predictedJump));
    }
    return false;
  }
}
