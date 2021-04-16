package uwu.narumi.deobfuscator.transformer.impl;

import java.util.HashSet;
import java.util.Set;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.DeobfuscationException;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class TrashExceptionRemoveTransformer implements Transformer {

  @Override
  public void transform(Deobfuscator deobfuscator) throws DeobfuscationException {
    deobfuscator.getClasses().stream()
        .flatMap(classNode -> classNode.methods.stream())
        .filter(methodNode -> methodNode.exceptions != null)
        .filter(methodNode -> !methodNode.exceptions.isEmpty())
        .forEach(methodNode -> {
          Set<String> toRemove = new HashSet<>();
          methodNode.exceptions.forEach(exception -> {
            if (methodNode.tryCatchBlocks.stream().noneMatch(node -> node.type.equals(exception))) {
              toRemove.add(exception);
            }
          });
          methodNode.exceptions.removeAll(toRemove);
          toRemove.clear();
        });
  }

  @Override
  public int weight() {
    return 1;
  }

  @Override
  public String name() {
    return "Trash Exception Remover";
  }
}
