package uwu.narumi.deobfuscator.transformer.composed.qprotect.v1_7_2;

import uwu.narumi.deobfuscator.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.transformer.Transformer;
import uwu.narumi.deobfuscator.transformer.impl.qprotect.latest.qProtectNumberTransformer;
import uwu.narumi.deobfuscator.transformer.impl.qprotect.v1_7_2.qProtectNumberPoolTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UnHideTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.LocalVariableRemoveTransformer;

import java.util.Arrays;
import java.util.List;

public class qProtectTransformer extends ComposedTransformer {

    @Override
    public List<Transformer> transformers() {
        return Arrays.asList(
                new qProtectNumberTransformer(),
                new qProtectNumberPoolTransformer(),
                new LocalVariableRemoveTransformer(),
                new UnHideTransformer()
        );
    }
}
