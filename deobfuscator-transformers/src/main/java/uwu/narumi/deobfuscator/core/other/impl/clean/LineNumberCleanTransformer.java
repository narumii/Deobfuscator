package uwu.narumi.deobfuscator.core.other.impl.clean;

import java.util.Arrays;

import org.objectweb.asm.tree.LineNumberNode;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class LineNumberCleanTransformer extends Transformer {

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      Arrays.stream(methodNode.instructions.toArray())
          .filter(node -> node instanceof LineNumberNode)
          .forEach(node -> {
            methodNode.instructions.remove(node);
            this.markChange();
          });
    }));
  }
}
