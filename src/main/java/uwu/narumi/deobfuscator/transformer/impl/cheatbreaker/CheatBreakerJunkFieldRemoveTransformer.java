package uwu.narumi.deobfuscator.transformer.impl.cheatbreaker;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class CheatBreakerJunkFieldRemoveTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes()
                .forEach(classNode -> classNode.fields.removeIf(field -> field.name.startsWith("__junk")));
    }
}
