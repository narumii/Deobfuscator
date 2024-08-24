package uwu.narumi.deobfuscator.core.other.impl.pool;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Inlines constant local variables
 */
public class InlineLocalVariablesTransformer extends Transformer {
  public InlineLocalVariablesTransformer() {
    this.rerunOnChange = true;
  }

  private boolean changed = false;

  @Override
  protected boolean transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      inlineLocalVariables(classWrapper, methodNode);
    }));

    return changed;
  }

  private void inlineLocalVariables(ClassWrapper classWrapper, MethodNode methodNode) {
    Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames = analyzeOriginalSource(classWrapper.getClassNode(), methodNode);
    if (frames == null) return;

    // Inline static local variables
    for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
      if (insn.getOpcode() == ILOAD) {
        VarInsnNode varInsn = (VarInsnNode) insn;

        Frame<OriginalSourceValue> frame = frames.get(insn);
        if (frame == null) continue;

        // ISTORE
        OriginalSourceValue storeVarSourceValue = frame.getLocal(varInsn.var);
        // Value reference
        OriginalSourceValue valueSourceValue = storeVarSourceValue.copiedFrom;
        if (valueSourceValue == null || !valueSourceValue.originalSource.isOneWayProduced() || storeVarSourceValue.getProducer().getOpcode() != ISTORE) continue;

        // Original source value on which we can operate
        AbstractInsnNode valueInsn = valueSourceValue.originalSource.getProducer();

        if (valueInsn.isConstant()) {
          AbstractInsnNode clone = valueInsn.clone(null);
          methodNode.instructions.set(insn, clone);

          changed = true;
        }
      }
    }
  }
}
