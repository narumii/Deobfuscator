package uwu.narumi.deobfuscator.transformer.composed;

import uwu.narumi.deobfuscator.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.transformer.Transformer;
import uwu.narumi.deobfuscator.transformer.impl.radon.RadonPackerTransformer;
import uwu.narumi.deobfuscator.transformer.impl.radon.RadonStringPoolTransformer;
import uwu.narumi.deobfuscator.transformer.impl.radon.RadonStringTransformer;

import java.util.Arrays;
import java.util.List;

public class RadonTransformer extends ComposedTransformer {

    //TODO
    @Override
    public List<Transformer> transformers() {
        return Arrays.asList(
                new RadonPackerTransformer(),

                new RadonStringTransformer(),
                new RadonStringPoolTransformer()
        );
    }
}
