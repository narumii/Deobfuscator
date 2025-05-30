package uwu.narumi.deobfuscator.core.other.impl.pool;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.VarLoadMatch;
import uwu.narumi.deobfuscator.api.helper.FramedInstructionsStream;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.HashMap;
import java.util.Map;

/**
 * Inlines constant local variables
 */
public class InlineLocalVariablesTransformer extends Transformer {
  private static final Match VAR_STORE_LOAD_PAIR = SequenceMatch.of(
      // Store
      Match.of(ctx -> ctx.insn().isVarStore()).capture("store"),
      // Load
      Match.of(ctx -> ctx.insn().isVarLoad()).capture("load")
  );

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

    // Find all variable store and load pairs
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      // Count var usages
      Map<Integer, Integer> varUsages = new HashMap<>(); // var index -> usage count
      for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
        if (insn instanceof VarInsnNode && insn.isVarLoad()) {
          varUsages.merge(((VarInsnNode) insn).var, 1, Integer::sum);
        } else if (insn instanceof IincInsnNode) {
          varUsages.merge(((IincInsnNode) insn).var, 1, Integer::sum);
        }
      }

      // Now we can find pairs
      MethodContext methodContext = MethodContext.of(classWrapper, methodNode);
      VAR_STORE_LOAD_PAIR.findAllMatches(methodContext).forEach(match -> {
        VarInsnNode varLoadInsn = (VarInsnNode) match.captures().get("load").insn();
        VarInsnNode varStoreInsn = (VarInsnNode) match.captures().get("store").insn();
        if (varLoadInsn.var != varStoreInsn.var) {
          // If they are not the same variable, we cannot inline
          return;
        }
        int varIndex = varLoadInsn.var;
        if (varUsages.getOrDefault(varIndex, 0) > 1) {
          // If the variable is used more than once, we cannot inline
          return;
        }

        // Remove the variable completely and use stack value instead
        match.removeAll();

        markChange();
      });
    }));
  }
}
