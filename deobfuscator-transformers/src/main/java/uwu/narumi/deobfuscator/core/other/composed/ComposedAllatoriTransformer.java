package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.impl.allatori.AllatoriStringTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;

public class ComposedAllatoriTransformer extends ComposedTransformer {

    public ComposedAllatoriTransformer(boolean strongString) {
        super(
            UniversalNumberTransformer::new,
            () -> new AllatoriStringTransformer(strongString)
        );
    }
}
