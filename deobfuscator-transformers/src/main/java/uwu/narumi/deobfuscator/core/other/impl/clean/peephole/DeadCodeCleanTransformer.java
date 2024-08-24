package uwu.narumi.deobfuscator.core.other.impl.clean.peephole;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.Frame;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class DeadCodeCleanTransformer extends Transformer {
  private boolean changed = false;

  @Override
  protected boolean transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).forEach(classWrapper -> {
      var iterator = classWrapper.methods().iterator();
      while (iterator.hasNext()) {
        MethodNode methodNode = iterator.next();

        Analyzer<?> analyzer = new Analyzer<>(new BasicInterpreter());
        try {
          analyzer.analyze(classWrapper.name(), methodNode);
        } catch (AnalyzerException e) {
          // Remove invalid method
          LOGGER.warn("Found invalid method: {}#{}{}. Removing...", classWrapper.name(), methodNode.name, methodNode.desc);
          iterator.remove();
          return;
        }
        Frame<?>[] frames = analyzer.getFrames();
        AbstractInsnNode[] insns = methodNode.instructions.toArray();
        for (int i = 0; i < frames.length; i++) {
          AbstractInsnNode insn = insns[i];

          // If frame is null then it means that the code is unreachable
          if (frames[i] == null && insn.getType() != AbstractInsnNode.LABEL) {
            methodNode.instructions.remove(insn);
            insns[i] = null;
            changed = true;
          }
        }
      }
    });

    return changed;
  }
}
