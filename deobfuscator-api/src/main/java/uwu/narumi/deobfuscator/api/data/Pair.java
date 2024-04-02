package uwu.narumi.deobfuscator.api.data;

import java.util.function.BiConsumer;

public record Pair<A, B>(A key, B value) {
  public static <X, Y> Pair<X, Y> of(X key, Y value) {
    return new Pair<>(key, value);
  }

  public void consume(BiConsumer<A, B> consumer) {
    consumer.accept(key(), value());
  }

  public boolean isPresent() {
    return key != null && value != null;
  }
}
