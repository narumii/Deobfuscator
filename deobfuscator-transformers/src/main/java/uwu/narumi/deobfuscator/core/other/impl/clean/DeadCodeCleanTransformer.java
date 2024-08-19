package uwu.narumi.deobfuscator.core.other.impl.clean;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.Frame;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Arrays;
import java.util.List;

public class DeadCodeCleanTransformer extends Transformer {

  private boolean changed = false;

  @Override
  protected boolean transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      removeDeadCode(classWrapper, methodNode);

      try {
        changed |= new UnUsedLabelCleanTransformer().transform(classWrapper, context);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      removeUselessGotoJumps(methodNode);
    }));

    return changed;
  }

  // Remove useless GOTO jumps
  /*
  A:
    ...
    goto B
  B:
    ...
   */
  private void removeUselessGotoJumps(MethodNode methodNode) {
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
            changed = true;
          }
        }
      }
    }
  }

  private void removeDeadCode(ClassWrapper classWrapper, MethodNode methodNode) {
    Analyzer<?> analyzer = new Analyzer<>(new BasicInterpreter());
    try {
      analyzer.analyze(classWrapper.name(), methodNode);
    } catch (AnalyzerException e) {
      // Ignore
      return;
    }
    Frame<?>[] frames = analyzer.getFrames();
    AbstractInsnNode[] insns = methodNode.instructions.toArray();
    for (int i = 0; i < frames.length; i++) {
      AbstractInsnNode insn = insns[i];
      if (frames[i] == null && insn.getType() != AbstractInsnNode.LABEL) {
        methodNode.instructions.remove(insn);
        insns[i] = null;
        changed = true;
      }
    }
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
