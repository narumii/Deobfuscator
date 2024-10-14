package uwu.narumi.deobfuscator.core.other.impl.pool;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.helper.FramedInstructionsStream;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

/**
 * Inlines constant local variables
 */
public class InlineLocalVariablesTransformer extends Transformer {
  @Override
  protected void transform() throws Exception {
    FramedInstructionsStream.of(this)
        .editInstructionsStream(stream -> stream.filter(AbstractInsnNode::isVarLoad))
        .forEach(insnContext -> {
          VarInsnNode varInsn = (VarInsnNode) insnContext.insn();

          // Var store instruction
          OriginalSourceValue storeVarSourceValue = insnContext.frame().getLocal(varInsn.var);
          // Value reference
          OriginalSourceValue valueSourceValue = storeVarSourceValue.copiedFrom;
          if (valueSourceValue == null || !valueSourceValue.originalSource.isOneWayProduced() || !storeVarSourceValue.getProducer().isVarStore()) return;

          // Original source value on which we can operate
          AbstractInsnNode valueInsn = valueSourceValue.originalSource.getProducer();

          if (valueInsn.isConstant()) {
            AbstractInsnNode clone = valueInsn.clone(null);
            insnContext.methodNode().instructions.set(insnContext.insn(), clone);

            markChange();
          }
        });
  }
}
