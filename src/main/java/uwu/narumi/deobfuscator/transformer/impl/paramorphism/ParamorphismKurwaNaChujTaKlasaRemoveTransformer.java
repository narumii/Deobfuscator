package uwu.narumi.deobfuscator.transformer.impl.paramorphism;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class ParamorphismKurwaNaChujTaKlasaRemoveTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.getClasses().keySet().removeIf(name -> name.startsWith("/////////////////////"));
    }
}
