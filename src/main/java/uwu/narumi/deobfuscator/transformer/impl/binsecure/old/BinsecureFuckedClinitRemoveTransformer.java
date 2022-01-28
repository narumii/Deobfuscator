package uwu.narumi.deobfuscator.transformer.impl.binsecure.old;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class BinsecureFuckedClinitRemoveTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> classNode.methods.removeIf(methodNode -> methodNode.name.equals("<clinit>") && !methodNode.desc.equals("()V")));
    }
}
