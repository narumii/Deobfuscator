package uwu.narumi.deobfuscator.core.other.impl.clean.peephole;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Map;

public class DeadCodeCleanTransformer extends Transformer {

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames = AsmHelper.analyzeSource(classWrapper.getClassNode(), methodNode);

      for (var entry : frames.entrySet()) {
        AbstractInsnNode insn = entry.getKey();
        Frame<OriginalSourceValue> frame = entry.getValue();
        if (frame == null && insn.getType() != AbstractInsnNode.LABEL) {
          // Remove unreachable instruction
          methodNode.instructions.remove(insn);
          this.markChange();
        }
      }
    }));
  }
}
