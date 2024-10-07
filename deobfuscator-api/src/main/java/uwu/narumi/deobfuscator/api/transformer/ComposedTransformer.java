package uwu.narumi.deobfuscator.api.transformer;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;

public class ComposedTransformer extends Transformer {

  private final List<Supplier<@Nullable Transformer>> transformers;

  @SafeVarargs
  public ComposedTransformer(Supplier<@Nullable Transformer>... transformers) {
    this.transformers = List.of(transformers);
  }

  @SafeVarargs
  public ComposedTransformer(boolean rerunOnChange, Supplier<Transformer>... transformers) {
    this.transformers = List.of(transformers);
    this.rerunOnChange = rerunOnChange;
  }

  @Override
  protected void transform(ClassWrapper scope, Context context) {
    transformers.forEach(transformerSupplier -> {
      boolean changed = Transformer.transform(transformerSupplier, scope, context);
      if (changed) {
        this.markChange();
      }
    });
  }
}
