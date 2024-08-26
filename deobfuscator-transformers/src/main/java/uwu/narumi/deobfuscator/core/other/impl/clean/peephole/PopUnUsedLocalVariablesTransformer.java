package uwu.narumi.deobfuscator.core.other.impl.clean.peephole;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PopUnUsedLocalVariablesTransformer extends Transformer {
  private final AtomicInteger removedVars = new AtomicInteger(0);

  @Override
  protected boolean transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames = AsmHelper.analyzeOriginalSource(classWrapper.getClassNode(), methodNode);
      if (frames == null) return;

      List<VarInsnNode> varStoresInUse = new ArrayList<>();

      // Find all local variables in use
      for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
        if (insn instanceof VarInsnNode varInsnNode && insn.isVarLoad()) {
          Frame<OriginalSourceValue> frame = frames.get(insn);
          if (frame == null) return;

          OriginalSourceValue localVariableSourceValue = frame.getLocal(varInsnNode.var);
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

            removedVars.incrementAndGet();
          }
        }
      }
    }));

    LOGGER.info("Removed {} unused local variables", removedVars.get());

    return removedVars.get() > 0;
  }
}
