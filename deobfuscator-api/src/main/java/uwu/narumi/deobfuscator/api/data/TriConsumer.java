package uwu.narumi.deobfuscator.api.data;

import java.util.Objects;

@FunctionalInterface
public interface TriConsumer<A, B, C> {
  void accept(A first, B second, C third);

  default TriConsumer<A, B, C> andThen(TriConsumer<? super A, ? super B, ? super C> after) {
    Objects.requireNonNull(after);
    return (first, second, third) -> {
      accept(first, second, third);
      after.accept(first, second, third);
    };
  }
}
