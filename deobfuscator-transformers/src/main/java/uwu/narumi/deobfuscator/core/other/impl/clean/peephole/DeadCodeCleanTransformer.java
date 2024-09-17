package uwu.narumi.deobfuscator.core.other.impl.clean.peephole;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Map;

public class DeadCodeCleanTransformer extends Transformer {

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).parallelStream().forEach(classWrapper -> classWrapper.methods().parallelStream().forEach(methodNode -> {
      // We want to use here the traditional analyzer. We want the true dead code.
      Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
      try {
        analyzer.analyze(classWrapper.name(), methodNode);
      } catch (AnalyzerException e) {
        throw new RuntimeException(e);
      }

      Frame<SourceValue>[] frames = analyzer.getFrames();

      AbstractInsnNode[] insns = methodNode.instructions.toArray();
      for (int i = 0; i < insns.length; i++) {
        AbstractInsnNode insn = insns[i];
        Frame<SourceValue> frame = frames[i];
        if (frame == null && insn.getType() != AbstractInsnNode.LABEL) {
          // Remove unreachable instruction
          methodNode.instructions.remove(insn);
          this.markChange();
        }
      }
    }));
  }
}
