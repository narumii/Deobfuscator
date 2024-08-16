package uwu.narumi.deobfuscator.core.other.impl.pool;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.StackWalker;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Inlines constant local variables
 */
public class InlineLocalVariablesTransformer extends Transformer {
  @Override
  public void transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      Map<AbstractInsnNode, Frame<SourceValue>> frames = analyzeSource(classWrapper.getClassNode(), methodNode);
      if (frames == null) return;

      List<AbstractInsnNode> toRemove = new ArrayList<>();

      // Inline static local variables
      for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
        if (insn.getOpcode() == ILOAD) {
          VarInsnNode varInsn = (VarInsnNode) insn;

          Frame<SourceValue> frame = frames.get(insn);
          if (frame == null) continue;

          // Get value from stack that is passed to ILOAD
          Optional<StackWalker.StackValue> valueInsn = StackWalker.getOnlyOnePossibleProducer(frames, frame.getLocal(varInsn.var));
          if (valueInsn.isEmpty()) continue;

          if (valueInsn.get().value().isConstant()) {
            AbstractInsnNode clone = valueInsn.get().value().clone(null);
            methodNode.instructions.set(insn, clone);
            valueInsn.get().removeWithVarInit(methodNode, toRemove);
          }
        }
      }

      // Cleanup
      toRemove.forEach(methodNode.instructions::remove);
    }));
  }
}
