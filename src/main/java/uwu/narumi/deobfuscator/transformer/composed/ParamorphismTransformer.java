package uwu.narumi.deobfuscator.transformer.composed;

import uwu.narumi.deobfuscator.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.transformer.Transformer;
import uwu.narumi.deobfuscator.transformer.impl.paramorphism.*;

import java.util.Arrays;
import java.util.List;

public class ParamorphismTransformer extends ComposedTransformer {

    /*private final String invokeDynamicClass;
    private final String[] stringObfuscationClasses;

    public ParamorphismTransformer(String invokeDynamicClass, String... stringObfuscationClasses) {
        this.invokeDynamicClass = invokeDynamicClass;
        this.stringObfuscationClasses = stringObfuscationClasses;
    }*/

    @Override
    public List<Transformer> transformers() {
        return Arrays.asList(
                new ParamorphismPackerTransformer(),
                new ParamorphismStringTransformer(
                        //stringObfuscationClasses
                ),
                new ParamorphismInvokeDynamicTransformer(
                        // invokeDynamicClass
                ),
                new ParamorphismFlowTransformer(),
                new ParamorphismKurwaNaChujTaKlasaRemoveTransformer()
        );
    }
}
