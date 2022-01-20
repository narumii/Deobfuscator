package uwu.narumi.deobfuscator.transformer.composed;

import uwu.narumi.deobfuscator.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.transformer.Transformer;
import uwu.narumi.deobfuscator.transformer.impl.cheatbreaker.CheatBreakerJunkFieldRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.cheatbreaker.CheatBreakerStaticArrayStringPoolTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UniversalNumberTransformer;

import java.util.Arrays;
import java.util.List;

public class CheatBreakerTransformer extends ComposedTransformer {

    @Override
    public List<Transformer> transformers() {
        return Arrays.asList(
                new UniversalNumberTransformer(),
                new CheatBreakerJunkFieldRemoveTransformer(),
                new CheatBreakerStaticArrayStringPoolTransformer()
        );
    }
}
