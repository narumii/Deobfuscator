package uwu.narumi.deobfuscator.api.asm.matcher;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.asm.matcher.result.Result;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;
import uwu.narumi.deobfuscator.api.data.Pair;
import uwu.narumi.deobfuscator.api.data.TriConsumer;
import uwu.narumi.deobfuscator.api.data.TriFunction;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;

/*
Fucking boilerplate will kill me someday
 */
public class InstructionMatcher {

  private final List<Match> matches;
  private final List<Match> skipMatches;

  private final MethodNode methodNode;

  public InstructionMatcher(List<Match> matches, List<Match> skipMatches, MethodNode methodNode) {
    this.matches = matches;
    this.skipMatches = skipMatches;
    this.methodNode = methodNode;
  }

  private InstructionMatcher(InstructionMatcher.Builder builder) {
    this.matches = builder.matches;
    this.skipMatches = builder.skipMatches;
    this.methodNode = null;
  }

  public static InstructionMatcher.Builder builder() {
    return new InstructionMatcher.Builder();
  }

  public InstructionMatcher bind(MethodNode methodNode) {
    return new InstructionMatcher(matches, skipMatches, methodNode);
  }

  public void replaceAll(TriConsumer<Result, List<Match>, ReplaceAction> consumer) {
    if (methodNode == null) throw new IllegalArgumentException();

    Map<AbstractInsnNode, InsnList> replacements = new HashMap<>();
    collect()
        .forEach(
            result -> {
              ReplaceAction replaceAction = new ReplaceAction();
              consumer.accept(result, matches, replaceAction);

              replaceAction.indexInstructionMap.forEach(
                  (index, instructions) -> {
                    AbstractInsnNode node = result.get(index);
                    if (replacements.containsKey(node))
                      throw new IllegalArgumentException("Replace actions overlaps");

                    replacements.put(node, AsmHelper.from(instructions));
                  });

              replaceAction.matchInstructionMap.forEach(
                  (match, instructions) -> {
                    for (AbstractInsnNode node : result.nodes()) {
                      if (!match.test(node)) continue;

                      if (replacements.containsKey(node))
                        throw new IllegalArgumentException("Replace actions overlaps");

                      replacements.put(node, AsmHelper.from(instructions));
                    }
                  });

              replaceAction.rangeIndexInstructionMap.forEach(
                  (range, instructions) -> {
                    for (int i = range.key(); i < range.value() - 1; i++) {
                      AbstractInsnNode node = result.get(i);
                      if (replacements.containsKey(node))
                        throw new IllegalArgumentException("Replace actions overlaps");

                      replacements.put(node, AsmHelper.from(instructions));
                    }
                  });
            });

    replacements.forEach(
        (node, insnList) -> {
          methodNode.instructions.insertBefore(node, insnList);
          methodNode.instructions.remove(node);
        });

    replacements.clear();
  }

  public void modifyAll(BiConsumer<Result, MethodNode> consumer) {
    if (methodNode == null) throw new IllegalArgumentException();

    collect().forEach(result -> consumer.accept(result, methodNode));
  }

  public void modifyFirst(BiConsumer<Result, MethodNode> consumer) {
    if (methodNode == null) throw new IllegalArgumentException();

    first().ifPresent(result -> consumer.accept(result, methodNode));
  }

  public void modifyLast(BiConsumer<Result, MethodNode> consumer) {
    if (methodNode == null) throw new IllegalArgumentException();

    last().ifPresent(result -> consumer.accept(result, methodNode));
  }

  public void replaceAll(AbstractInsnNode... nodes) {
    if (methodNode == null || nodes == null || nodes.length == 0)
      throw new IllegalArgumentException();

    InsnList insnList = new InsnList();
    for (AbstractInsnNode node : nodes) {
      insnList.add(node);
    }

    collect()
        .forEach(
            result -> {
              methodNode.instructions.insert(result.start(), AsmHelper.copy(insnList));
              for (AbstractInsnNode node : result.nodes()) {
                methodNode.instructions.remove(node);
              }
            });
  }

  public void replaceFirst(AbstractInsnNode... nodes) {
    if (methodNode == null || nodes == null || nodes.length == 0)
      throw new IllegalArgumentException();

    InsnList insnList = new InsnList();
    for (AbstractInsnNode node : nodes) {
      insnList.add(node);
    }

    first()
        .ifPresent(
            result -> {
              methodNode.instructions.insert(result.start(), AsmHelper.copy(insnList));
              for (AbstractInsnNode node : result.nodes()) {
                methodNode.instructions.remove(node);
              }
            });
  }

  public void replaceLast(AbstractInsnNode... nodes) {
    if (methodNode == null || nodes == null || nodes.length == 0)
      throw new IllegalArgumentException();

    InsnList insnList = new InsnList();
    for (AbstractInsnNode node : nodes) {
      insnList.add(node);
    }

    last()
        .ifPresent(
            result -> {
              methodNode.instructions.insert(result.start(), AsmHelper.copy(insnList));
              for (AbstractInsnNode node : result.nodes()) {
                methodNode.instructions.remove(node);
              }
            });
  }

  public void removeAll() {
    if (methodNode == null) throw new IllegalArgumentException();

    collect().stream()
        .flatMap(result -> Arrays.stream(result.nodes()))
        .forEach(methodNode.instructions::remove);
  }

  public void removeFirst() {
    if (methodNode == null) throw new IllegalArgumentException();

    first()
        .map(Result::nodes)
        .ifPresent(
            nodes -> {
              for (AbstractInsnNode node : nodes) {
                methodNode.instructions.remove(node);
              }
            });
  }

  public void removeLast() {
    if (methodNode == null) throw new IllegalArgumentException();

    last()
        .map(Result::nodes)
        .ifPresent(
            nodes -> {
              for (AbstractInsnNode node : nodes) {
                methodNode.instructions.remove(node);
              }
            });
  }

  public Optional<Result> last() {
    if (methodNode == null) throw new IllegalArgumentException();

    AbstractInsnNode current = methodNode.instructions.getFirst();
    if (current == null) return Optional.empty();

    AtomicReference<Result> result = new AtomicReference<>();
    loop(
        current,
        (node, insideCurrent, nodes) -> {
          result.set(new Result(node, insideCurrent, methodNode, nodes));
          return false;
        });

    return Optional.ofNullable(result.get());
  }

  public Optional<Result> first() {
    if (methodNode == null) throw new IllegalArgumentException();

    AbstractInsnNode current = methodNode.instructions.getFirst();
    if (current == null) return Optional.empty();

    AtomicReference<Result> result = new AtomicReference<>();
    loop(
        current,
        (node, insideCurrent, nodes) -> {
          result.set(new Result(node, insideCurrent, methodNode, nodes));
          return true;
        });

    return Optional.ofNullable(result.get());
  }

  public boolean contains() {
    if (methodNode == null) throw new IllegalArgumentException();

    AbstractInsnNode current = methodNode.instructions.getFirst();
    if (current == null) return false;

    AtomicBoolean atomicBoolean = new AtomicBoolean();
    loop(
        current,
        (node, insideCurrent, nodes) -> {
          atomicBoolean.set(true);
          return true;
        });

    return atomicBoolean.get();
  }

  public List<Result> collect() {
    if (methodNode == null) throw new IllegalArgumentException();

    List<Result> results = new ArrayList<>();
    AbstractInsnNode current = methodNode.instructions.getFirst();
    if (current == null) return List.of();

    loop(
        methodNode.instructions.getFirst(),
        (node, insideCurrent, nodes) -> {
          results.add(new Result(node, insideCurrent, methodNode, nodes));
          return false;
        });

    return results;
  }

  public void loop(
      AbstractInsnNode current,
      TriFunction<AbstractInsnNode, AbstractInsnNode, AbstractInsnNode[], Boolean> function) {
    do {
      AbstractInsnNode node = current;
      if (skipMatches.stream().anyMatch(match -> match.test(node))) {
        current = node.getNext();
        continue;
      }

      AbstractInsnNode insideCurrent = current;
      AbstractInsnNode[] nodes = new AbstractInsnNode[matches.size()];

      int pos = 0;
      do {
        AbstractInsnNode finalInsideCurrent = insideCurrent;
        if (matches.get(pos).test(finalInsideCurrent)) {
          nodes[pos++] = finalInsideCurrent;
        } else if (skipMatches.stream().anyMatch(match -> match.test(finalInsideCurrent))) {
          insideCurrent = insideCurrent.getNext();
          continue;
        } else {
          break;
        }

        if (pos >= matches.size()) break;

        insideCurrent = insideCurrent.getNext();
      } while (insideCurrent != null);

      if (pos == matches.size() && function.apply(node, insideCurrent, nodes)) {
        return;
      }

      current = node.getNext();
    } while (current != null);
  }

  public boolean match(AbstractInsnNode start) {
    int matched = 0, executed = 0;
    do {
      AbstractInsnNode current = start;
      if (current == null) {
        break;
      } else if (matches.get(matched).test(current)) {
        matched++;
      } else if (skipMatches.stream().noneMatch(match -> match.test(current))) {
        executed++;
      }

      start = start.getNext();
    } while (executed + matched < matches.size());

    return matched == matches.size();
  }

  public void match(AbstractInsnNode start, Consumer<Result> consumer) {
    int matched = 0, executed = 0;
    AbstractInsnNode[] nodes = new AbstractInsnNode[matches.size()];
    do {
      AbstractInsnNode current = start;
      if (current == null) {
        break;
      } else if (matches.get(matched).test(current)) {
        nodes[matched++] = current;
      } else if (skipMatches.stream().noneMatch(match -> match.test(current))) {
        executed++;
      }

      start = start.getNext();
    } while (executed + matched < matches.size());

    if (matched == nodes.length) {
      consumer.accept(new Result(nodes[0], nodes[matched - 1], methodNode, nodes));
    }
  }

  public boolean backwardsMatch(AbstractInsnNode start) {
    int matched = matches.size(), executed = 0;
    do {
      int position = matched - 1;
      AbstractInsnNode current = start;
      if (current == null) {
        break;
      } else if (matches.get(position).test(current)) {
        --matched;
      } else if (skipMatches.stream().noneMatch(match -> match.test(current))) {
        executed++;
      }

      start = start.getPrevious();
    } while (matched != 0 || matched + executed != matches.size());

    return matched == 0;
  }

  public void backwardsMatch(AbstractInsnNode start, Consumer<Result> consumer) {
    int matched = matches.size(), executed = 0;
    AbstractInsnNode[] nodes = new AbstractInsnNode[matched];
    do {
      int position = matched - 1;
      AbstractInsnNode current = start;
      if (current == null) {
        break;
      } else if (matches.get(position).test(current)) {
        nodes[position] = current;
        --matched;
      } else if (skipMatches.stream().noneMatch(match -> match.test(current))) {
        executed++;
      }

      start = start.getPrevious();
    } while (matched != 0 || matched + executed > matches.size());

    if (matched == 0) {
      consumer.accept(new Result(nodes[0], nodes[nodes.length - 1], methodNode, nodes));
    }
  }

  public static class ReplaceAction {
    private final Map<Integer, AbstractInsnNode[]> indexInstructionMap = new HashMap<>();
    private final Map<Match, AbstractInsnNode[]> matchInstructionMap = new HashMap<>();

    private final Map<Pair<Integer, Integer>, AbstractInsnNode[]> rangeIndexInstructionMap =
        new HashMap<>();

    public void put(int index, AbstractInsnNode... nodes) {
      indexInstructionMap.put(index, nodes);
    }

    public void put(int start, int end, AbstractInsnNode... nodes) {
      rangeIndexInstructionMap.put(Pair.of(start, end), nodes);
    }

    public void put(Match match, AbstractInsnNode... nodes) {
      matchInstructionMap.put(match, nodes);
    }
  }

  public static class CompiledInstructionMatcher {
    private final BiConsumer<Result, MethodNode> consumer;
    private final InstructionMatcher instructionMatcher;

    private CompiledInstructionMatcher(Builder builder, BiConsumer<Result, MethodNode> consumer) {
      this.instructionMatcher = new InstructionMatcher(builder);
      this.consumer = consumer;
    }

    public boolean invoke(MethodNode methodNode) {
      AtomicBoolean invoked = new AtomicBoolean();
      instructionMatcher
          .bind(methodNode)
          .modifyAll(
              ((result, methodNode1) -> {
                consumer.accept(result, methodNode1);
                invoked.set(true);
              }));

      return invoked.get();
    }
  }

  public static class Builder {

    private static final Match FRAME_MATCH = node -> node instanceof FrameNode;
    private static final Match LABEL_MATCH = node -> node instanceof LabelNode;
    private static final Match LINE_MATCH = node -> node instanceof LineNumberNode;

    private final List<Match> matches = new ArrayList<>();
    private final List<Match> skipMatches = new ArrayList<>();

    private Builder() {
      skipMatches.addAll(List.of(FRAME_MATCH, LABEL_MATCH, LINE_MATCH));
    }

    public InstructionMatcher.Builder matches(Match... matches) {
      this.matches.addAll(Arrays.asList(matches));
      return this;
    }

    public InstructionMatcher.Builder skip(Match... matches) {
      this.skipMatches.addAll(Arrays.asList(matches));
      return this;
    }

    public InstructionMatcher.Builder doNotSkipFrames() {
      this.skipMatches.remove(FRAME_MATCH);
      return this;
    }

    public InstructionMatcher.Builder doNotSkipLabels() {
      this.skipMatches.remove(LABEL_MATCH);
      return this;
    }

    public InstructionMatcher.Builder doNotSkipLineNumbers() {
      this.skipMatches.remove(LINE_MATCH);
      return this;
    }

    public InstructionMatcher.Builder doNotSkip() {
      doNotSkipLabels();
      doNotSkipLabels();
      doNotSkipFrames();
      return this;
    }

    public InstructionMatcher build() {
      return new InstructionMatcher(this);
    }

    public CompiledInstructionMatcher build(BiConsumer<Result, MethodNode> consumer) {
      return new CompiledInstructionMatcher(this, consumer);
    }
  }
}
