import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.impl.sb27.*;

import java.nio.file.Path;

public class Loader {

    public static void main(String... args) throws Exception {
        Deobfuscator.builder()
                .input(Path.of("example/Evaluator-obf.jar"))
                .output(Path.of("example/Evaluator-deobf.jar"))
                .transformers(
                        new SuperblaubeereNumberTransformer(),
                        new SuperblaubeereNumberPoolTransformer(),
                        new SuperblaubeereFlowTransformer(),
                        new SuperblaubeereSourceInfoStringTransformer(),
                        new SuperblaubeereStringTransformer(),
                        new SuperblaubeereStringPoolTransformer(),
                        new SuperblaubeereInvokeDynamicTransformer(),

                        new SuperblaubeerePackagerTransformer(),

                        new SuperblaubeereNumberTransformer(),
                        new SuperblaubeereNumberPoolTransformer(),
                        new SuperblaubeereFlowTransformer(),
                        new SuperblaubeereSourceInfoStringTransformer(),
                        new SuperblaubeereStringTransformer(),
                        new SuperblaubeereStringPoolTransformer(),
                        new SuperblaubeereInvokeDynamicTransformer()
                )
                .classReaderFlags(ClassReader.SKIP_FRAMES)
                .classWriterFlags(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES)
                .build()
                .start();

    }
}
