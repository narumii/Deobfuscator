package uwu.narumi.deobfuscator.core.other.impl.pool;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.InsnContext;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.helper.FramedInstructionsStream;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Inlines local variables (constants and next to each other var load/store pairs).
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

    // Inline var load/store pairs that are next to each other and used only once
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      MethodContext methodContext = MethodContext.of(classWrapper, methodNode);

      // Count var usages
      Map<VarInsnNode, Integer> varUsages = AsmHelper.getVarUsages(methodContext);

      for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
        if (insn.isVarStore()) {
          if (!varUsages.containsKey((VarInsnNode) insn) || varUsages.get((VarInsnNode) insn) > 1) {
            // If the variable is used more than once, we cannot inline
            continue;
          }

          List<VarInsnNode> varStoreOrder = new ArrayList<>();
          // Collect all variable indexes in order of their usage
          AbstractInsnNode end = insn.followJumpsUntil(insn2 -> {
            if (insn2.isVarStore()) {
              return AbstractInsnNode.PredicateResult.CONTINUE;
            }
            return AbstractInsnNode.PredicateResult.SUCCESS;
          }, AbstractInsnNode::isVarStore, insn2 -> varStoreOrder.add((VarInsnNode)insn2));
          if (end == null) {
            // If we cannot find the end of the sequence, we cannot inline
            continue;
          }

          // Check if we have var load instructions in the same order
          List<VarInsnNode> varLoadOrder = new ArrayList<>();
          end.followJumpsUntil(insn2 -> {
            if (insn2.isVarLoad()) {
              InsnContext insnContext = methodContext.at(insn2);
              OriginalSourceValue localVariableSourceValue = insnContext.frame().getLocal(((VarInsnNode) insn2).var);
              if (!localVariableSourceValue.isOneWayProduced()) {
                // If the variable is not one-way produced, we cannot inline
                return AbstractInsnNode.PredicateResult.FAILED;
              }
              return AbstractInsnNode.PredicateResult.CONTINUE;
            }

            return AbstractInsnNode.PredicateResult.SUCCESS;
          }, AbstractInsnNode::isVarLoad, insn2 -> varLoadOrder.add((VarInsnNode)insn2));

          List<Integer> varStoreIndexesOrder = varStoreOrder.stream()
              .map(varInsn -> varInsn.var)
              .toList();
          // In reverse order to match the stack order
          List<Integer> varLoadIndexesOrder = varLoadOrder.stream()
              .map(varInsn -> varInsn.var)
              .sorted(Collections.reverseOrder())
              .toList();

          // Var load and stores are in the same order and next to each other. We can inline them
          if (varStoreIndexesOrder.equals(varLoadIndexesOrder)) {
            // Inline (use stack instead of var load/store)
            varStoreOrder.forEach(varInsn -> methodNode.instructions.remove(varInsn));
            varLoadOrder.forEach(varInsn -> methodNode.instructions.remove(varInsn));

            markChange();
          }
        }
      }
    }));
  }
}
