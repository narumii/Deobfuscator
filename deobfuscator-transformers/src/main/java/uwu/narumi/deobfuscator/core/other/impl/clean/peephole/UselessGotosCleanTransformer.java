package uwu.narumi.deobfuscator.core.other.impl.clean.peephole;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Arrays;
import java.util.List;

/**
 * Remove useless GOTO jumps
 * <pre>
 * A:
 *   ...
 *   goto B
 * B:
 *   ...
 * </pre>
 */
public class UselessGotosCleanTransformer extends Transformer {

  @Override
  protected void transform() throws Exception {
    scopedClasses().parallelStream().forEach(classWrapper -> classWrapper.methods().parallelStream().forEach(methodNode -> {
      for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
        if (insn.getOpcode() == GOTO) {
          JumpInsnNode jumpInsn = (JumpInsnNode) insn;
          if (jumpInsn.getNext() instanceof LabelNode labelNode && jumpInsn.label == labelNode) {

            // Check if the label is used only by the jump instruction
            if (!isLabelUsedOnlyByInstructions(methodNode, jumpInsn.label)) continue;

            List<AbstractInsnNode> labelUsedInsns = Arrays.stream(methodNode.instructions.toArray())
                .filter(newInsn -> checkIfJumpHappens(newInsn, labelNode))
                .toList();

            boolean labelUsedOnlyOnce = labelUsedInsns.size() == 1;

            if (labelUsedOnlyOnce) {
              // Remove the goto and the label
              methodNode.instructions.remove(labelNode);
              methodNode.instructions.remove(jumpInsn);
              this.markChange();
            }
          }
        }
      }
    }));
  }

  private boolean isLabelUsedOnlyByInstructions(MethodNode methodNode, LabelNode labelNode) {
    if (methodNode.tryCatchBlocks != null) {
      boolean usedByTryCatchBlock = methodNode.tryCatchBlocks.stream()
          .anyMatch(tryCatchBlockNode -> tryCatchBlockNode.start == labelNode || tryCatchBlockNode.end == labelNode || tryCatchBlockNode.handler == labelNode);

      if (usedByTryCatchBlock) {
        return false;
      }
    }

    if (methodNode.localVariables != null) {
      boolean usedByLocalVariable = methodNode.localVariables.stream()
          .anyMatch(localVariableNode -> localVariableNode.start == labelNode || localVariableNode.end == labelNode);

      if (usedByLocalVariable) {
        return false;
      }
    }
    return true;
  }

  private boolean checkIfJumpHappens(AbstractInsnNode insn, LabelNode labelNode) {
    if (insn instanceof JumpInsnNode jumpInsn) {
      return jumpInsn.label == labelNode;
    } else if (insn instanceof LookupSwitchInsnNode lookupSwitchInsn) {
      return lookupSwitchInsn.labels.contains(labelNode) || lookupSwitchInsn.dflt == labelNode;
    } else if (insn instanceof TableSwitchInsnNode tableSwitchInsn) {
      return tableSwitchInsn.labels.contains(labelNode) || tableSwitchInsn.dflt == labelNode;
    }
    return false;
  }
}
