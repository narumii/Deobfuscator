package uwu.narumi.deobfuscator.core.other.impl.universal.number;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.helper.FramedInstructionsStream;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.HashSet;
import java.util.Set;

/**
 * Inline constant values into instructions.
 */
public class InlineConstantValuesTransformer extends Transformer {
  @Override
  protected void transform() throws Exception {
    Set<AbstractInsnNode> poppedInsns = new HashSet<>();

    FramedInstructionsStream.of(this)
        .forceSync()
        .forEach(insnContext -> {
          int stackCount = insnContext.getRequiredStackValuesCount();

          // Iterate over stack values
          for (int i = 0; i < stackCount; i++) {
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
        });
  }
}
