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
import uwu.narumi.deobfuscator.api.transformer.FramedInstructionsTransformer;

/**
 * Simplify LOOKUPSWITCH and TABLESWITCH instructions
 */
public class SwitchFlowTransformer extends FramedInstructionsTransformer {

  @Override
  protected boolean transformInstruction(ClassWrapper classWrapper, MethodNode methodNode, AbstractInsnNode insn, Frame<OriginalSourceValue> frame) {
    if (insn.getOpcode() == LOOKUPSWITCH) {
      LookupSwitchInsnNode lookupSwitchInsn = (LookupSwitchInsnNode) insn;

      OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);
      if (!sourceValue.originalSource.isOneWayProduced()) return false;

      AbstractInsnNode valueInsn = sourceValue.originalSource.getProducer();
      if (valueInsn.isInteger()) {
        int value = valueInsn.asInteger();
        int index = lookupSwitchInsn.keys.indexOf(value);

        if (index == -1) {
          // Jump to default
          methodNode.instructions.set(lookupSwitchInsn, new JumpInsnNode(GOTO, lookupSwitchInsn.dflt));
        } else {
          // Match found! Jump to target
          LabelNode targetLabel = lookupSwitchInsn.labels.get(index);
          methodNode.instructions.set(lookupSwitchInsn, new JumpInsnNode(GOTO, targetLabel));
        }
        // Remove value from stack
        methodNode.instructions.remove(sourceValue.getProducer());

        return true;
      }
    } else if (insn.getOpcode() == TABLESWITCH) {
      TableSwitchInsnNode tableSwitchInsn = (TableSwitchInsnNode) insn;

      OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);
      if (!sourceValue.originalSource.isOneWayProduced()) return false;

      AbstractInsnNode valueInsn = sourceValue.originalSource.getProducer();
      if (valueInsn.isInteger()) {
        int value = valueInsn.asInteger();
        int index = value - tableSwitchInsn.min;

        if (index < 0 || index >= tableSwitchInsn.labels.size()) {
          // Jump to default
          methodNode.instructions.set(tableSwitchInsn, new JumpInsnNode(GOTO, tableSwitchInsn.dflt));
        } else {
          // Match found! Jump to target
          LabelNode targetLabel = tableSwitchInsn.labels.get(index);
          methodNode.instructions.set(tableSwitchInsn, new JumpInsnNode(GOTO, targetLabel));
        }
        // Remove value from stack
        methodNode.instructions.remove(sourceValue.getProducer());

        return true;
      }
    }
    return false;
  }
}
