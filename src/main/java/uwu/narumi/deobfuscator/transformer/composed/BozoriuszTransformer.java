package uwu.narumi.deobfuscator.transformer.composed;

import uwu.narumi.deobfuscator.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.transformer.Transformer;
import uwu.narumi.deobfuscator.transformer.impl.bozoriusz.*;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.RefreshTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.TryCatchFixTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UniversalNumberTransformer;

import java.util.Arrays;
import java.util.List;

public class BozoriuszTransformer extends ComposedTransformer {

    @Override
    public List<Transformer> transformers() {
        return Arrays.asList(
                new BozoriuszShitRemoverTransformer(),
                new UniversalNumberTransformer(false),
                new BozoriuszConstantFlowTransformer(),
                new RefreshTransformer(),
                new BozoriuszStringTransformer(),
                new BozoriuszLightControlFlowTransformer(),
                new BozoriuszHeavyControlFlowTransformer(false),
                new RefreshTransformer(),
                new BozoriuszHeavyControlFlowTransformer(true),
                new TryCatchFixTransformer()
        );
    }
}
