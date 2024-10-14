package uwu.narumi.deobfuscator.core.other.impl.clean;

import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

/**
 * Remove invalid methods. WARNING: If some transformer produces invalid bytecode in methods, this transformer will remove them.
 */
public class InvalidMethodCleanTransformer extends Transformer {

  @Override
  protected void transform() throws Exception {
    scopedClasses().parallelStream().forEach(classWrapper -> {
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
          markChange();
        }
      }
    });
  }
}
