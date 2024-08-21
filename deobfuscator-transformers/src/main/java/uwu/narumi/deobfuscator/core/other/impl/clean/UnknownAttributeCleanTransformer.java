package uwu.narumi.deobfuscator.core.other.impl.clean;

import org.objectweb.asm.Attribute;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class UnknownAttributeCleanTransformer extends Transformer {

  private boolean changed = false;

  @Override
  protected boolean transform(ClassWrapper scope, Context context) throws Exception {
    context
        .classes(scope)
        .forEach(
            classWrapper -> {
              changed |= classWrapper.getClassNode().attrs.removeIf(Attribute::isUnknown);
              classWrapper
                  .methods()
                  .forEach(methodNode -> changed |= methodNode.attrs.removeIf(Attribute::isUnknown));
              classWrapper
                  .fields()
                  .forEach(fieldNode -> changed |= fieldNode.attrs.removeIf(Attribute::isUnknown));
            });

    return changed;
  }
}
