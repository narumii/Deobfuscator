package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.api.transformer.Transformer;
import uwu.narumi.deobfuscator.core.other.impl.allatori.AllatoriString;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;

import java.util.Arrays;
import java.util.List;

public class ComposedAllatoriStrongStringTransformer extends ComposedTransformer {

    @Override
    public List<Transformer> transformers() {
        return Arrays.asList(
                new UniversalNumberTransformer(),
                new AllatoriString(true));
    }
}
