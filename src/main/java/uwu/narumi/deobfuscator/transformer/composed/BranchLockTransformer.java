package uwu.narumi.deobfuscator.transformer.composed;

import uwu.narumi.deobfuscator.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.transformer.Transformer;
import uwu.narumi.deobfuscator.transformer.impl.branchlock.BranchLockCompatibilityStringTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.StackOperationTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UnHideTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UniversalNumberTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.SignatureRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.UnknownAttributeRemoveTransformer;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class BranchLockTransformer extends ComposedTransformer {

    @Override
    public List<Transformer> transformers() {
        File file = new File("branchlock_string_lookup.txt");
        if (file.exists()) file.delete();
        return Arrays.asList(
                new SignatureRemoveTransformer(),
//                new BranchLockReferenceTransformer(),
//                new LocalVariableRemoveTransformer(),
                new UnknownAttributeRemoveTransformer(),
                new UnHideTransformer(),
                new UniversalNumberTransformer(),
                new StackOperationTransformer(),
//                new BranchLockNumberTransformer(),
                new UniversalNumberTransformer(),
                new BranchLockCompatibilityStringTransformer(),
//                new BranchLockNumberTransformer(),
                new UniversalNumberTransformer()
//                new BranchLockFlowTransformer()
        );
    }
}
