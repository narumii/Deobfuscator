package uwu.narumi.deobfuscator.transformer.impl;

import java.util.Arrays;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.DeobfuscationException;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class TrashInstructionsRemoveTransformer implements Transformer {

  @Override
  public void transform(Deobfuscator deobfuscator) throws DeobfuscationException {
    deobfuscator.getClasses().stream()
        .flatMap(classNode -> classNode.methods.stream())
        .forEach(methodNode -> {

          Arrays.stream(methodNode.instructions.toArray())
              .filter(node -> node.getOpcode() == NOP)
              .forEach(node -> methodNode.instructions.remove(node));

          Arrays.stream(methodNode.instructions.toArray())
              .filter(node -> node.getOpcode() == ATHROW)
              .filter(node -> node.getPrevious().getOpcode() == ATHROW)
              .forEach(node -> {
                methodNode.instructions.remove(node.getPrevious());
                methodNode.instructions.remove(node);
              });
        });
  }

  @Override
  public int weight() {
    return 3;
  }

  @Override
  public String name() {
    return "Trash Instructions Remover";
  }
}
