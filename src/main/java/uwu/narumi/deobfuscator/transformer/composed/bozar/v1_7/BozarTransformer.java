package uwu.narumi.deobfuscator.transformer.composed.bozar.v1_7;

import java.util.Arrays;
import java.util.List;

import uwu.narumi.deobfuscator.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.transformer.Transformer;
import uwu.narumi.deobfuscator.transformer.impl.bozar.BozarHeavyConstantFlowTransformer;
import uwu.narumi.deobfuscator.transformer.impl.bozar.BozarStringTransformer;
import uwu.narumi.deobfuscator.transformer.impl.bozar.v1_7.BozarLightFlowTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UniversalNumberTransformer;

public class BozarTransformer extends ComposedTransformer {

    @Override
    public List<Transformer> transformers() {
    	return Arrays.asList(
    			new UniversalNumberTransformer(),
    			new BozarHeavyConstantFlowTransformer(),
        		new BozarLightFlowTransformer(),
        		new BozarStringTransformer());
    }

}
