package uwu.narumi.deobfuscator.transformer.impl;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.DeobfuscationException;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class UnHideCodeTransformer implements Transformer {

  @Override
  public void transform(Deobfuscator deobfuscator) throws DeobfuscationException {
    deobfuscator.getClasses().forEach(classNode -> {
      if (ASMHelper.has(classNode.access, ACC_SYNTHETIC)) {
        classNode.access &= ~ACC_SYNTHETIC;
      }

      if (ASMHelper.has(classNode.access, ACC_DEPRECATED)) {
        classNode.access &= ~ACC_DEPRECATED;
      }

      classNode.fields.forEach(fieldNode -> {
        if (ASMHelper.has(fieldNode.access, ACC_DEPRECATED)) {
          fieldNode.access &= ~ACC_DEPRECATED;
        }

        if (ASMHelper.has(fieldNode.access, ACC_SYNTHETIC)) {
          fieldNode.access &= ~ACC_SYNTHETIC;
        }
      });

      classNode.methods.forEach(methodNode -> {
        if (ASMHelper.has(methodNode.access, ACC_SYNTHETIC)) {
          methodNode.access &= ~ACC_SYNTHETIC;
        }

        if (ASMHelper.has(methodNode.access, ACC_BRIDGE)) {
          methodNode.access &= ~ACC_BRIDGE;
        }

        if (ASMHelper.has(methodNode.access, ACC_DEPRECATED)) {
          methodNode.access &= ~ACC_DEPRECATED;
        }
      });
    });
  }

  @Override
  public int weight() {
    return 0;
  }

  @Override
  public String name() {
    return "Code UnHider";
  }
}
