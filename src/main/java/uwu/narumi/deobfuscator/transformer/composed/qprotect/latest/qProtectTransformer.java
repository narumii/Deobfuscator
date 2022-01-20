package uwu.narumi.deobfuscator.transformer.composed.qprotect.latest;

import uwu.narumi.deobfuscator.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.transformer.Transformer;
import uwu.narumi.deobfuscator.transformer.impl.monsey.MonseyFakeTryCatchTransformer;
import uwu.narumi.deobfuscator.transformer.impl.qprotect.b31.qProtectStringTransformer;
import uwu.narumi.deobfuscator.transformer.impl.qprotect.latest.qProtectFieldFlowTransformer;
import uwu.narumi.deobfuscator.transformer.impl.qprotect.latest.qProtectFlowTransformer;
import uwu.narumi.deobfuscator.transformer.impl.qprotect.latest.qProtectInvokeDynamicTransformer;
import uwu.narumi.deobfuscator.transformer.impl.qprotect.latest.qProtectNumberTransformer;
import uwu.narumi.deobfuscator.transformer.impl.sb27.SuperblaubeereFlowTransformer;
import uwu.narumi.deobfuscator.transformer.impl.sb27.SuperblaubeereNumberPoolTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.TryCatchFixTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UnHideTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UniversalNumberTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.SignatureRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.UnknownAttributeRemoveTransformer;

import java.util.Arrays;
import java.util.List;

public class qProtectTransformer extends ComposedTransformer {

    @Override
    public List<Transformer> transformers() {
        return Arrays.asList(
                new UniversalNumberTransformer(),
                new qProtectNumberTransformer(),
                new qProtectFieldFlowTransformer(),
                new MonseyFakeTryCatchTransformer(),
                new qProtectFlowTransformer(),
                new qProtectNumberTransformer(),
                new SuperblaubeereFlowTransformer(),
                new qProtectFlowTransformer(),
                new qProtectStringTransformer(),
                new TryCatchFixTransformer(),
                new qProtectInvokeDynamicTransformer(),
                new uwu.narumi.deobfuscator.transformer.impl.qprotect.latest.qProtectStringTransformer(),
                new UnknownAttributeRemoveTransformer(),
                new SignatureRemoveTransformer(),
                new UnHideTransformer(),
                new SuperblaubeereNumberPoolTransformer()
        );
    }
}
