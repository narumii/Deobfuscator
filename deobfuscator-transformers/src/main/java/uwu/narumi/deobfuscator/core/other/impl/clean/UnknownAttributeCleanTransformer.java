package uwu.narumi.deobfuscator.core.other.impl.clean;

import org.objectweb.asm.Attribute;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class UnknownAttributeCleanTransformer extends Transformer {

  @Override
  public void transform(ClassWrapper scope, Context context) throws Exception {
    context
        .classes(scope)
        .forEach(
            classWrapper -> {
              classWrapper.getClassNode().attrs.removeIf(Attribute::isUnknown);
              classWrapper
                  .methods()
                  .forEach(methodNode -> methodNode.attrs.removeIf(Attribute::isUnknown));
              classWrapper
                  .fields()
                  .forEach(fieldNode -> fieldNode.attrs.removeIf(Attribute::isUnknown));
            });
  }
}
