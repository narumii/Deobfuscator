package uwu.narumi.deobfuscator.transformer.composed;

import uwu.narumi.deobfuscator.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.transformer.Transformer;
import uwu.narumi.deobfuscator.transformer.impl.qprotect.latest.qProtectFieldFlowTransformer;
import uwu.narumi.deobfuscator.transformer.impl.scuti.ScutiInvokeDynamicTransformer;
import uwu.narumi.deobfuscator.transformer.impl.scuti.ScutiNormalStringTransformer;
import uwu.narumi.deobfuscator.transformer.impl.scuti.ScutiStrongStringTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.StackOperationTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.TryCatchFixTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UniversalNumberTransformer;

import java.util.Arrays;
import java.util.List;

public class ScutiTransformer extends ComposedTransformer {

    // https://github.com/netindev/scuti/blob/467b856b7ea46009608ccdf4db69b4b43e640fa6/scuti-core/src/main/java/tk/netindev/scuti/core/transform/obfuscation/StringEncryptionTransformer.java#L188
    // https://github.com/netindev/scuti/blob/467b856b7ea46009608ccdf4db69b4b43e640fa6/scuti-core/src/main/java/tk/netindev/scuti/core/transform/obfuscation/StringEncryptionTransformer.java#L148
    private final boolean isStrongStringType;

    public ScutiTransformer(boolean isStrongStringType) {
        this.isStrongStringType = isStrongStringType;
    }

    @Override
    public List<Transformer> transformers() {
        return Arrays.asList(
                new StackOperationTransformer(),
                new UniversalNumberTransformer(),
                new qProtectFieldFlowTransformer(),
                new ScutiInvokeDynamicTransformer(),
                new StackOperationTransformer(),
                new UniversalNumberTransformer(),

                (isStrongStringType ? new ScutiStrongStringTransformer() : new ScutiNormalStringTransformer()),

                new TryCatchFixTransformer()
        );
    }
}
