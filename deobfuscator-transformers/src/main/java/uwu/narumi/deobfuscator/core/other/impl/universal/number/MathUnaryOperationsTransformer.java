package uwu.narumi.deobfuscator.core.other.impl.universal.number;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.helper.AsmMathHelper;
import uwu.narumi.deobfuscator.api.helper.FramedInstructionsStream;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

/**
 * Simplifies number casts on constant value.
 */
public class MathUnaryOperationsTransformer extends Transformer {
  @Override
  protected void transform() throws Exception {
    FramedInstructionsStream.of(this)
        .editInstructionsStream(stream -> stream.filter(insn -> AsmMathHelper.isMathUnaryOperation(insn.getOpcode())))
        .forEach(insnContext -> {
          // Get instructions from stack that are passed
          OriginalSourceValue sourceValue = insnContext.frame().getStack(insnContext.frame().getStackSize() - 1);
          OriginalSourceValue originalSource = sourceValue.originalSource;
          if (!originalSource.isOneWayProduced()) return;

          AbstractInsnNode valueInsn = originalSource.getProducer();

          if (valueInsn.isNumber()) {
            Number castedNumber = AsmMathHelper.mathUnaryOperation(valueInsn.asNumber(), insnContext.insn().getOpcode());

            insnContext.placePops();
            insnContext.methodNode().instructions.set(insnContext.insn(), AsmHelper.numberInsn(castedNumber));

            markChange();
          }
        });
  }
}
