package uwu.narumi.deobfuscator.api.transformer;

import java.util.List;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;

public abstract class ComposedTransformer extends Transformer {

  @Override
  public void transform(ClassWrapper scope, Context context) {
    transformers()
        .forEach(
            transformer -> {
              try {
                transformer.transform(scope, context);
              } catch (Exception e) {
                LOGGER.error("Error while using {}", transformer.name(), e);
              }
            });
  }

  public abstract List<Transformer> transformers();
}
