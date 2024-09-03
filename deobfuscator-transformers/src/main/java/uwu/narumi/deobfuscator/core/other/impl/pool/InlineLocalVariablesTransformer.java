package uwu.narumi.deobfuscator.core.other.impl.pool;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.FramedInstructionsTransformer;

import java.util.stream.Stream;

/**
 * Inlines constant local variables
 */
public class InlineLocalVariablesTransformer extends FramedInstructionsTransformer {
  public InlineLocalVariablesTransformer() {
    this.rerunOnChange = true;
  }

  @Override
  protected Stream<AbstractInsnNode> buildInstructionsStream(Stream<AbstractInsnNode> stream) {
    return stream
        .filter(AbstractInsnNode::isVarLoad);
  }

  @Override
  protected boolean transformInstruction(Context context, ClassWrapper classWrapper, MethodNode methodNode, AbstractInsnNode insn, Frame<OriginalSourceValue> frame) {
    VarInsnNode varInsn = (VarInsnNode) insn;

    // Var store instruction
    OriginalSourceValue storeVarSourceValue = frame.getLocal(varInsn.var);
    // Value reference
    OriginalSourceValue valueSourceValue = storeVarSourceValue.copiedFrom;
    if (valueSourceValue == null || !valueSourceValue.originalSource.isOneWayProduced() || !storeVarSourceValue.getProducer().isVarStore()) return false;

    // Original source value on which we can operate
    AbstractInsnNode valueInsn = valueSourceValue.originalSource.getProducer();

    if (valueInsn.isConstant()) {
      AbstractInsnNode clone = valueInsn.clone(null);
      methodNode.instructions.set(insn, clone);

      return true;
    }

    return false;
  }
}
