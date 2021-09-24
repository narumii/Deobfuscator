import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.impl.binsecure.old.FuckedClinitRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.binsecure.old.OldBinsecureFlowTransformer;

import java.nio.file.Path;

public class Loader {

    public static void main(String... args) throws Exception {
        Deobfuscator.builder()
                .input(Path.of("example/binsecure_0.4/Evaluator-flow.jar"))
                .output(Path.of("example/binsecure_0.4/Evaluator-flow-deobf.jar"))
                .transformers(
                        new FuckedClinitRemoveTransformer(),
                        new OldBinsecureFlowTransformer()
                )
                .classReaderFlags(ClassReader.SKIP_FRAMES)
                .classWriterFlags(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES)
                .build()
                .start();

    }
}
