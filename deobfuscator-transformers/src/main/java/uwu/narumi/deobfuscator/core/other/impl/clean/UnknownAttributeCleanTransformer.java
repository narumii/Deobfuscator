package uwu.narumi.deobfuscator.core.other.impl.clean;

import org.objectweb.asm.Attribute;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class UnknownAttributeCleanTransformer extends Transformer {

  private boolean changed = false;

  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).forEach(classWrapper -> {
      if (classWrapper.classNode().attrs != null) {
        changed |= classWrapper.classNode().attrs.removeIf(Attribute::isUnknown);
      }
      classWrapper.methods().forEach(methodNode -> {
        if (methodNode.attrs != null) {
          changed |= methodNode.attrs.removeIf(Attribute::isUnknown);
        }
      });
      classWrapper.fields().forEach(fieldNode -> {
        if (fieldNode.attrs != null) {
          changed |= fieldNode.attrs.removeIf(Attribute::isUnknown);
        }
      });
    });

    if (changed) {
      markChange();
    }
  }
}
