package uwu.narumi.deobfuscator.core.other.impl.clean.peephole;

import java.util.Arrays;

import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class NopCleanTransformer extends Transformer {

  @Override
  protected void transform() throws Exception {
    scopedClasses().stream()
        .flatMap(classWrapper -> classWrapper.methods().stream())
        .forEach(
            methodNode ->
                Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node.getOpcode() == NOP)
                    .forEach(node -> {
                      methodNode.instructions.remove(node);
                      this.markChange();
                    }));
  }
}
