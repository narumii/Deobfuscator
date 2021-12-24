package uwu.narumi.deobfuscator.transformer.composed;

import uwu.narumi.deobfuscator.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.transformer.Transformer;
import uwu.narumi.deobfuscator.transformer.impl.monsey.MonseyFakeJumpTransformer;
import uwu.narumi.deobfuscator.transformer.impl.monsey.MonseyFakeTryCatchTransformer;
import uwu.narumi.deobfuscator.transformer.impl.monsey.MonseyStringTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UnHideTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.InvalidAnnotationRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.NopRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.OkThrowRemoveTransformer;

import java.util.Arrays;
import java.util.List;

public class MonseyTransformer extends ComposedTransformer {

    @Override
    public List<Transformer> transformers() {
        return Arrays.asList(
                new MonseyStringTransformer(),
                new MonseyFakeJumpTransformer(),
                new MonseyFakeTryCatchTransformer(),
                new OkThrowRemoveTransformer(),
                new NopRemoveTransformer(),
                new UnHideTransformer(),
                new InvalidAnnotationRemoveTransformer()
        );
    }
}
