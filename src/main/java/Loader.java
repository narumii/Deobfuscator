import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.impl.binsecure.latest.BinsecureInvokeDynamicCallTransformer;
import uwu.narumi.deobfuscator.transformer.impl.binsecure.latest.BinsecureInvokeDynamicFieldTransformer;
import uwu.narumi.deobfuscator.transformer.impl.binsecure.latest.BinsecureInvokeDynamicVariableTransformer;

import java.nio.file.Path;

public class Loader {

    public static void main(String... args) throws Exception {
        Deobfuscator.builder()
                .input(Path.of("example/binsecure 1.8.3/Evaluator-obf.jar"))
                .output(Path.of("example/binsecure 1.8.3/Evaluator-deobf.jar"))
                .transformers(
                        new BinsecureInvokeDynamicCallTransformer(),
                        new BinsecureInvokeDynamicFieldTransformer(),
                        new BinsecureInvokeDynamicVariableTransformer()
                )
                .classReaderFlags(ClassReader.SKIP_FRAMES)
                .classWriterFlags(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES)
                .build()
                .start();

    }
}
