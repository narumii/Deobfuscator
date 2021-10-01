import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.impl.binsecure.old.*;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.LineNumberRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.LocalVariableRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.UnUsedLabelNodeRemoveTransformer;

import java.nio.file.Path;

public class Loader {

    public static void main(String... args) throws Exception {
        Deobfuscator.builder()
                .input(Path.of("example/binsecure_0.4/Evaluator-obf.jar"))
                .output(Path.of("example/binsecure_0.4/Evaluator-deobf.jar"))
                .transformers(
                        new FuckedClinitRemoveTransformer(),
                        new OldBinecureNumberTransformer(),
                        new LineNumberRemoveTransformer(),
                        new LocalVariableRemoveTransformer(),
                        new OldBinsecureFlowTransformer(),
                        new UnUsedLabelNodeRemoveTransformer(),
                        new OldBinsecureFlowTransformer(),
                        new OldBinsecureCrasherRemoveTransformer(),
                        new OldBinsecureStringTransformer()
                )
                .classReaderFlags(ClassReader.SKIP_FRAMES)
                .classWriterFlags(ClassWriter.COMPUTE_MAXS)
                .build()
                .start();

    }
}
