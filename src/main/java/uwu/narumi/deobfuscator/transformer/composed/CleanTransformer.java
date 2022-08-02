package uwu.narumi.deobfuscator.transformer.composed;

import uwu.narumi.deobfuscator.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.transformer.Transformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.TryCatchFixTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UnHideTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.*;

import java.util.Arrays;
import java.util.List;

public class CleanTransformer extends ComposedTransformer {

    @Override
    public List<Transformer> transformers() {
        return Arrays.asList(
                new ImageCrasherRemoveTransformer(),
                new LineNumberRemoveTransformer(),
                new TryCatchFixTransformer(),
                new UnHideTransformer(),
                new InvalidAnnotationRemoveTransformer(),
                new MethodLocalsAndParametersRemoveTransformer(),
                new SignatureRemoveTransformer(),
                new SourceInfoRemoveTransformer()
        );
    }
}
