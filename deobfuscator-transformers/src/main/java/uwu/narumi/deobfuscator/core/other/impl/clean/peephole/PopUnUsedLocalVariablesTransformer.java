package uwu.narumi.deobfuscator.core.other.impl.clean.peephole;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.FramedMethodsTransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PopUnUsedLocalVariablesTransformer extends FramedMethodsTransformer {

  @Override
  protected void transformMethod(Context context, ClassWrapper classWrapper, MethodNode methodNode, Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames) {
    List<VarInsnNode> varStoresInUse = new ArrayList<>();

    // Find all local variables in use
    for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
      if ((insn instanceof VarInsnNode && !insn.isVarStore()) || insn instanceof IincInsnNode) {
        Frame<OriginalSourceValue> frame = frames.get(insn);
        if (frame == null) return;

        int varIndex;
        if (insn instanceof VarInsnNode varInsnNode) {
          varIndex = varInsnNode.var;
        } else {
          varIndex = ((IincInsnNode) insn).var;
        }

        OriginalSourceValue localVariableSourceValue = frame.getLocal(varIndex);
        for (AbstractInsnNode sourceInsn : localVariableSourceValue.insns) {
          // Save var stores in use
          if (sourceInsn.isVarStore()) {
            varStoresInUse.add((VarInsnNode) sourceInsn);
          }
        }
      }
    }

    // Remove all local variables that are not in use
    for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
      if (insn instanceof VarInsnNode varInsnNode && insn.isVarStore()) {
        if (!varStoresInUse.contains(varInsnNode)) {
          // Pop the value from the stack
          methodNode.instructions.set(insn, insn.toPop());

          this.markChange();
        }
      }
    }
  }

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    super.transform(scope, context);

    LOGGER.info("Popped {} unused local variables", this.getChangesCount());
  }
}
