package uwu.narumi.deobfuscator.core.other.impl.universal.number;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.InsnContext;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.HashSet;
import java.util.Set;

/**
 * Inline constant values into instructions.
 */
public class InlineConstantValuesTransformer extends Transformer {
  @Override
  protected void transform() throws Exception {
    scopedClasses().parallelStream().forEach(classWrapper -> classWrapper.methods().parallelStream().forEach(methodNode -> {
      // Very good workaround for inlining constants, yes yes :D
      // By placing labels before each instruction, we ensure that the analyzer
      // does not merge frames and we get more accurate stack values
      Set<LabelNode> tempLabels = new HashSet<>();
      for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
        LabelNode labelNode = new LabelNode();
        tempLabels.add(labelNode);
        methodNode.instructions.insert(insn, labelNode);
      }

      MethodContext methodContext = MethodContext.of(classWrapper, methodNode);

      Set<AbstractInsnNode> poppedInsns = new HashSet<>();
      for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
        InsnContext insnContext = methodContext.at(insn);
        if (insnContext.frame() == null) continue;

        // Iterate over stack values
        for (int i = 0; i < insnContext.frame().getStackSize(); i++) {
          // Using "originalSource" ensures that we don't replace DUPs or var loads
          OriginalSourceValue sourceValue = insnContext.frame().getStack(insnContext.frame().getStackSize() - (i + 1)).originalSource;

          // Check if the source value is constant
          if (sourceValue.getConstantValue() != null) {
            // Replace constant value with instruction
            AbstractInsnNode toReplace = sourceValue.getProducer();

            // Constants are already constants. Skip them
            if (toReplace.isConstant()) continue;

            if (poppedInsns.contains(toReplace)) {
              // Already popped
              continue;
            }

            // Place pops before replacing
            insnContext.methodContext().at(toReplace).placePops();

            // Replace instruction with constant value
            AbstractInsnNode newInsn = AsmHelper.toConstantInsn(sourceValue.getConstantValue().value());
            insnContext.methodNode().instructions.set(toReplace, newInsn);
            poppedInsns.add(toReplace);

            markChange();
          }
        }
      }

      // Remove temp labels
      tempLabels.forEach(labelNode -> methodNode.instructions.remove(labelNode));
    }));
  }
}
