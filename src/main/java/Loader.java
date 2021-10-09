import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.impl.qprotect.b31.qProtectStringTransformer;
import uwu.narumi.deobfuscator.transformer.impl.qprotect.b3_0.qProtectFlowTransformer;
import uwu.narumi.deobfuscator.transformer.impl.qprotect.b3_0.qProtectInvokeDynamicTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.TryCatchFixTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UniversalNumberTransformer;

import java.nio.file.Path;

public class Loader {

    public static void main(String... args) throws Exception {
        Deobfuscator.builder()
                .input(Path.of("example/qprotect/b31/Evaluator-obf.jar"))
                .output(Path.of("example/qprotect/b31/Evaluator-deobf.jar"))
                .transformers(
                        new UniversalNumberTransformer(),
                        new qProtectFlowTransformer(),
                        new TryCatchFixTransformer(),
                        new qProtectInvokeDynamicTransformer(),
                        new qProtectStringTransformer()
                )
                .classReaderFlags(ClassReader.SKIP_FRAMES)
                .classWriterFlags(ClassWriter.COMPUTE_MAXS)
                .build()
                .start();

    }
}
