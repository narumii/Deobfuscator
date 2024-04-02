package uwu.narumi.deobfuscator.api.data;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface TriFunction<A, B, C, R> {
  R apply(A first, B second, C third);

  default <V> TriFunction<A, B, C, V> andThen(Function<? super R, ? extends V> after) {
    Objects.requireNonNull(after);
    return (A first, B second, C third) -> after.apply(apply(first, second, third));
  }
}
