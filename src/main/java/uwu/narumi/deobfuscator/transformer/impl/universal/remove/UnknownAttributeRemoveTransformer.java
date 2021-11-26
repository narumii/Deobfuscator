package uwu.narumi.deobfuscator.transformer.impl.universal.remove;

import org.objectweb.asm.Attribute;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class UnknownAttributeRemoveTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            if (classNode.attrs != null) {
                classNode.attrs.removeIf(Attribute::isUnknown);
            }

            classNode.methods.forEach(methodNode -> {
                if (methodNode.attrs != null) {
                    methodNode.attrs.removeIf(Attribute::isUnknown);
                }
            });

            classNode.fields.forEach(fieldNode -> {
                if (fieldNode.attrs != null) {
                    fieldNode.attrs.removeIf(Attribute::isUnknown);
                }
            });
        });
    }
}
