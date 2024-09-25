package uwu.narumi.deobfuscator.core.other.impl.clean.peephole;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.InsnContext;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;

public class PopUnUsedLocalVariablesTransformer extends Transformer {

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      MethodContext methodContext = MethodContext.framed(classWrapper, methodNode);

      List<VarInsnNode> varStoresInUse = new ArrayList<>();

      // Find all local variables in use
      for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
        if ((insn instanceof VarInsnNode && !insn.isVarStore()) || insn instanceof IincInsnNode) {
          InsnContext insnContext = methodContext.newInsnContext(insn);

          Frame<OriginalSourceValue> frame = insnContext.frame();
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
            InsnContext insnContext = methodContext.newInsnContext(insn);

            // Pop the value from the stack
            insnContext.pop(1);

            methodNode.instructions.remove(insn);

            this.markChange();
          }
        }
      }
    }));

    LOGGER.info("Popped {} unused local variables", this.getChangesCount());
  }
}
