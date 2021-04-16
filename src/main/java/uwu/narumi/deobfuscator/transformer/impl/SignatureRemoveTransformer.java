package uwu.narumi.deobfuscator.transformer.impl;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.DeobfuscationException;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class SignatureRemoveTransformer implements Transformer {

  @Override
  public void transform(Deobfuscator deobfuscator) throws DeobfuscationException {
    deobfuscator.getClasses().forEach(classNode -> {
      classNode.signature = null;
      classNode.methods.forEach(methodNode -> methodNode.signature = null);
      classNode.fields.forEach(fieldNode -> fieldNode.signature = null);
    });
  }

  @Override
  public int weight() {
    return 0;
  }

  @Override
  public String name() {
    return "Signature Remover";
  }
}
