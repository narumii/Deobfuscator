package uwu.narumi.deobfuscator.transformer.impl.universal.remove;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class SignatureRemoveTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            classNode.signature = null;
            classNode.methods.forEach(methodNode -> methodNode.signature = null);
            classNode.fields.forEach(fieldNode -> fieldNode.signature = null);
        });
    }
}
