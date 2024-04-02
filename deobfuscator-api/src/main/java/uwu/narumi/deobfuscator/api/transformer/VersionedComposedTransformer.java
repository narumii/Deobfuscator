package uwu.narumi.deobfuscator.api.transformer;

import java.util.List;
import java.util.Map;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;

public abstract class VersionedComposedTransformer extends Transformer {

  private final String version;

  public VersionedComposedTransformer(String version) {
    this.version = version;
  }

  @Override
  public void transform(ClassWrapper scope, Context context) {
    Map<String, List<Transformer>> transformers = transformersByVersions();
    if (!transformers.containsKey(version)) {
      throw new IllegalArgumentException(String.format("Version '%s' not found!", version));
    }

    transformers
        .get(version)
        .forEach(
            transformer -> {
              try {
                transformer.transform(scope, context);
              } catch (Exception e) {
                LOGGER.error("Error while using {}", transformer.name(), e);
              }
            });
  }

  public abstract Map<String, List<Transformer>> transformersByVersions();
}
