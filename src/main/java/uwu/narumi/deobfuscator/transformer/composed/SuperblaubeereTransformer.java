package uwu.narumi.deobfuscator.transformer.composed;

import uwu.narumi.deobfuscator.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.transformer.Transformer;
import uwu.narumi.deobfuscator.transformer.impl.sb27.*;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UniversalNumberTransformer;

import java.util.Arrays;
import java.util.List;

public class SuperblaubeereTransformer extends ComposedTransformer {

    private final boolean isPacked;

    public SuperblaubeereTransformer(boolean isPacked) {
        this.isPacked = isPacked;
    }

    @Override
    public List<Transformer> transformers() {
        return !isPacked
                ?
                Arrays.asList(
                        new UniversalNumberTransformer(),
                        new SuperblaubeereNumberPoolTransformer(),
                        new SuperblaubeereFlowTransformer(),
                        new SuperblaubeereSourceInfoStringTransformer(),
                        new SuperblaubeereStringTransformer(),
                        new SuperblaubeereStringPoolTransformer(),
                        new SuperblaubeereInvokeDynamicTransformer()
                )
                :
                Arrays.asList(
                        new UniversalNumberTransformer(),
                        new SuperblaubeereNumberPoolTransformer(),
                        new SuperblaubeereFlowTransformer(),
                        new SuperblaubeereSourceInfoStringTransformer(),
                        new SuperblaubeereStringTransformer(),
                        new SuperblaubeereStringPoolTransformer(),
                        new SuperblaubeereInvokeDynamicTransformer(),

                        new SuperblaubeerePackagerTransformer(),

                        new UniversalNumberTransformer(),
                        new SuperblaubeereNumberPoolTransformer(),
                        new SuperblaubeereFlowTransformer(),
                        new SuperblaubeereSourceInfoStringTransformer(),
                        new SuperblaubeereStringTransformer(),
                        new SuperblaubeereStringPoolTransformer(),
                        new SuperblaubeereInvokeDynamicTransformer()
                );
    }
}
