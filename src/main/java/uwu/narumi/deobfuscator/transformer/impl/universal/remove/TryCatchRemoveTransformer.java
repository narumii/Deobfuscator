package uwu.narumi.deobfuscator.transformer.impl.universal.remove;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class TryCatchRemoveTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> methodNode.tryCatchBlocks = null);
    }
}
