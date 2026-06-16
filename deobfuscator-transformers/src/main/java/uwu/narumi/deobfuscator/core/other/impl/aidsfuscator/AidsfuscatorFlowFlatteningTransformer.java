package uwu.narumi.deobfuscator.core.other.impl.aidsfuscator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.helper.MethodHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AidsfuscatorFlowFlatteningTransformer extends Transformer {
  @Override
  protected void transform() {
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      if (methodNode.name.equals("<clinit>")) return;

      for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
        if (!(insn instanceof LookupSwitchInsnNode lookupSwitch)) continue;

        Dispatcher dispatcher = identifyDispatcher(lookupSwitch);
        if (dispatcher == null) continue;

        var frames = analyze(classWrapper.classNode(), methodNode);
        if (frames == null) continue;

        List<AbstractInsnNode> stores = new ArrayList<>();
        List<AbstractInsnNode> jumps = new ArrayList<>();
        boolean allMatch = true;

        for (LabelNode target : lookupSwitch.labels) {
          AbstractInsnNode jumpInsn = AsmHelper.previousMeaningful(target);
          if (!(jumpInsn instanceof JumpInsnNode jump) || jump.getOpcode() != GOTO || jump.label != dispatcher.label) {
            allMatch = false;
            break;
          }

          AbstractInsnNode storeInsn = AsmHelper.previousMeaningful(jumpInsn);
          if (!(storeInsn instanceof VarInsnNode store) || store.getOpcode() != ISTORE || store.var != dispatcher.local) {
            allMatch = false;
            break;
          }

          jumps.add(jumpInsn);
          stores.add(storeInsn);
        }

        if (!allMatch || stores.isEmpty()) continue;

        Set<AbstractInsnNode> toRemove = new HashSet<>(jumps);
        for (AbstractInsnNode storeInsn : stores) {
          MethodHelper.removeStackProducers(methodNode, storeInsn, frames, 1);
          toRemove.add(storeInsn);
        }

        toRemove.forEach(node -> {
          if (methodNode.instructions.contains(node)) {
            methodNode.instructions.remove(node);
          }
        });

        if (methodNode.instructions.contains(dispatcher.load)) {
          methodNode.instructions.remove(dispatcher.load);
        }
        if (methodNode.instructions.contains(lookupSwitch)) {
          methodNode.instructions.remove(lookupSwitch);
        }

        markChange();
      }
    }));

    LOGGER.info("Removed {} Aidsfuscator flow dispatcher groups", getChangesCount());
  }

  private Dispatcher identifyDispatcher(LookupSwitchInsnNode lookupSwitch) {
    AbstractInsnNode previous = AsmHelper.previousMeaningful(lookupSwitch);
    if (!(previous instanceof VarInsnNode load) || load.getOpcode() != ILOAD) return null;

    AbstractInsnNode raw = previous.getPrevious();
    while (raw != null && raw.getType() == AbstractInsnNode.FRAME) {
      raw = raw.getPrevious();
    }
    if (!(raw instanceof LabelNode label) || lookupSwitch.dflt != label) return null;

    if (lookupSwitch.labels == null || lookupSwitch.labels.size() < 3) return null;
    return new Dispatcher(label, load.var, load);
  }

  private java.util.Map<AbstractInsnNode, Frame<OriginalSourceValue>> analyze(ClassNode classNode, MethodNode methodNode) {
    try {
      return MethodHelper.analyzeSource(classNode, methodNode);
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private record Dispatcher(LabelNode label, int local, VarInsnNode load) {
  }
}
