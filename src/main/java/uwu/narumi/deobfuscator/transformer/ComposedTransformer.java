package uwu.narumi.deobfuscator.transformer;

import uwu.narumi.deobfuscator.Deobfuscator;

import java.util.List;

public abstract class ComposedTransformer extends Transformer {

    public abstract List<Transformer> transformers();

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.transform(transformers());
    }
}
