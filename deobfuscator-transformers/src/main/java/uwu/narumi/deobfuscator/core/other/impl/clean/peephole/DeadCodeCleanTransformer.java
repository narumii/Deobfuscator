package uwu.narumi.deobfuscator.core.other.impl.clean.peephole;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.FramedMethodsTransformer;

import java.util.Map;

public class DeadCodeCleanTransformer extends FramedMethodsTransformer {

  @Override
  protected void transformMethod(Context context, ClassWrapper classWrapper, MethodNode methodNode, Map<AbstractInsnNode, Frame<OriginalSourceValue>> frames) {
    for (var entry : frames.entrySet()) {
      AbstractInsnNode insn = entry.getKey();
      Frame<OriginalSourceValue> frame = entry.getValue();
      if (frame == null && insn.getType() != AbstractInsnNode.LABEL) {
        // Remove unreachable instruction
        methodNode.instructions.remove(insn);
        this.markChange();
      }
    }
  }
}
