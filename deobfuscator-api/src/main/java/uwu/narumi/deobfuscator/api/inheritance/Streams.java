package uwu.narumi.deobfuscator.api.inheritance;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Streams {
  public static <T> Stream<T> recurseWithoutCycles(T seed, Function<T, Set<T>> flatMap) {
    Deque<Iterator<T>> vertices = new ArrayDeque<>();
    Set<T> visited = new HashSet<>();
    vertices.push(Collections.singletonList(seed).iterator());
    return StreamSupport.stream(new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.NONNULL) {
      @Override
      public boolean tryAdvance(Consumer<? super T> action) {
        while (true) {
          Iterator<T> iterator = vertices.peek();
          if (iterator == null) {
            return false;
          }
          if (!iterator.hasNext()) {
            vertices.poll();
            continue;
          }
          T vertex = iterator.next();
          if (visited.add(vertex)) {
            action.accept(vertex);
            vertices.push(flatMap.apply(vertex).iterator());
            return true;
          }
        }
      }
    }, false);
  }
}
