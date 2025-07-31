package uwu.narumi.deobfuscator.core.other.impl.clean.peephole;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import uwu.narumi.deobfuscator.api.asm.InsnContext;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Set;

public class PopUnUsedLocalVariablesTransformer extends Transformer {

  @Override
  protected void transform() throws Exception {
    scopedClasses().parallelStream().forEach(classWrapper -> classWrapper.methods().parallelStream().forEach(methodNode -> {
      MethodContext methodContext = MethodContext.of(classWrapper, methodNode);

      Set<VarInsnNode> varStoresInUse = AsmHelper.getVarUsages(methodContext).keySet();

      // Remove all local variables that are not in use
      for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
        if (insn instanceof VarInsnNode varInsnNode && insn.isVarStore()) {
          if (!varStoresInUse.contains(varInsnNode)) {
            InsnContext insnContext = methodContext.at(insn);

            // Pop the value from the stack
            insnContext.placePops();

            methodNode.instructions.remove(insn);

            this.markChange();
          }
        }
      }
    }));

    LOGGER.info("Popped {} unused local variables", this.getChangesCount());
  }
}
