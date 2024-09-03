package uwu.narumi.deobfuscator.api.transformer;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;

public abstract class VersionedComposedTransformer extends Transformer {

  private final String version;

  public VersionedComposedTransformer(String version) {
    this.version = version;
  }

  private boolean changed = false;

  @Override
  protected void transform(ClassWrapper scope, Context context) {
    Map<String, List<Supplier<Transformer>>> transformers = transformersByVersions();
    if (!transformers.containsKey(version)) {
      throw new IllegalArgumentException(String.format("Version '%s' not found!", version));
    }

    transformers
        .get(version)
        .forEach(transformer -> changed |= Transformer.transform(transformer, scope, context));

    if (changed) {
      markChange();
    }
  }

  public abstract Map<String, List<Supplier<Transformer>>> transformersByVersions();
}
