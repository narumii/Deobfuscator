package uwu.narumi.deobfuscator.transformer.impl.universal.other;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class IPLoggerRemoveTransformer extends Transformer {

    /*
    YES
     */
    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> classNode.methods.removeIf(methodNode -> methodNode.name.contains("<html>")));
    }
}
