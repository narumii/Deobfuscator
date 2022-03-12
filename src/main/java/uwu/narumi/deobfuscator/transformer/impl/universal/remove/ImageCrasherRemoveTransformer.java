package uwu.narumi.deobfuscator.transformer.impl.universal.remove;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class ImageCrasherRemoveTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.getClasses().entrySet().removeIf(entry -> entry.getKey().contains("<html>"));
        deobfuscator.getFiles().entrySet().removeIf(entry -> entry.getKey().contains("<html>"));
    }
}
