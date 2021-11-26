package uwu.narumi.deobfuscator.transformer.impl.qprotect.latest;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;
import uwu.narumi.deobfuscator.transformer.impl.binsecure.BinsecureNumberTransformer;

public class qProtectNumberTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        new BinsecureNumberTransformer().transform(deobfuscator); //YES
    }
}
