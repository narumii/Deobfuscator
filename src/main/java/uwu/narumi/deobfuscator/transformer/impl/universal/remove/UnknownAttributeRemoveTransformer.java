package uwu.narumi.deobfuscator.transformer.impl.universal.remove;

import org.objectweb.asm.Attribute;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class UnknownAttributeRemoveTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            classNode.attrs.removeIf(Attribute::isUnknown);
            classNode.methods.forEach(methodNode -> methodNode.attrs.removeIf(Attribute::isUnknown));
            classNode.fields.forEach(fieldNode -> fieldNode.attrs.removeIf(Attribute::isUnknown));
        });
    }
}
