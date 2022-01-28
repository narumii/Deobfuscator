package uwu.narumi.deobfuscator.transformer.composed;

import uwu.narumi.deobfuscator.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.transformer.Transformer;
import uwu.narumi.deobfuscator.transformer.impl.binsecure.BinsecureNumberTransformer;
import uwu.narumi.deobfuscator.transformer.impl.binsecure.BinsecureSemiFlowTransformer;
import uwu.narumi.deobfuscator.transformer.impl.binsecure.latest.*;
import uwu.narumi.deobfuscator.transformer.impl.monsey.MonseyFakeTryCatchTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.TryCatchRemoveTransformer;

import java.util.Arrays;
import java.util.List;

public class BinsecureTransformer extends ComposedTransformer {

    //private final String stringKeysClassName;
    //private final String stringMapClassName;
    private final boolean useStackAnalyzer;

    public BinsecureTransformer(/*String stringKeysClassName, String stringMapClassName, */boolean useStackAnalyzer) {
        //this.stringKeysClassName = stringKeysClassName;
        //this.stringMapClassName = stringMapClassName;
        this.useStackAnalyzer = useStackAnalyzer;
    }

    @Override
    public List<Transformer> transformers() {
        return Arrays.asList(
                new BinsecureSemiMbaTransformer(),
                new BinsecureNumberTransformer(),
                new BinsecureInvokeDynamicFieldTransformer(),
                new BinsecureInvokeDynamicCallTransformer(),
                new BinsecureStringTransformer(/*stringKeysClassName, stringMapClassName,*/ useStackAnalyzer),
                new BinsecureSemiFlowTransformer(),
                new BinsecureNumberTransformer(),
                new BinsecureCrasherRemoveTransformer(),
                new MonseyFakeTryCatchTransformer(),
                new TryCatchRemoveTransformer()
        );
    }
}
