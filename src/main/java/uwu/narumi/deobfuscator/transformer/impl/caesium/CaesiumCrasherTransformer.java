package uwu.narumi.deobfuscator.transformer.impl.caesium;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class CaesiumCrasherTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.getClasses().entrySet().removeIf(entry -> entry.getKey().startsWith("<html><img src"));
    }
}

