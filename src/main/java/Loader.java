import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.impl.paramorphism.*;

import java.nio.file.Path;

public class Loader {

    public static void main(String... args) throws Exception {
        /*Deobfuscator.builder()
                .input(Path.of("example/paramorphism/paramorphism-2.1.2_9.jar"))
                .output(Path.of("example/paramorphism/paramorphism-2.1.2_9-deobf.jar"))
                .transformers(
                        new ParamorphismStringTransformer(
                                "\u0000paramorphism-obfuscator/s/a",
                                "\u0000paramorphism-obfuscator/s/l",
                                "\u0000paramorphism-obfuscator/s/m",
                                "\u0000paramorphism-obfuscator/s/v",
                                "\u0000paramorphism-obfuscator/s/Dispatcher"
                        ),
                        new ParamorphismInvokeDynamicTransformer(
                                "\u0000paramorphism-obfuscator/m/⛔$0"
                        ),
                        new ParamorphismFlowTransformer(),
                        new ParamorphismKurwaNaChujTaKlasaRemoveTransformer()
                )
                .classReaderFlags(ClassReader.SKIP_FRAMES)
                .classWriterFlags(ClassWriter.COMPUTE_MAXS)
                .build()
                .start();*/

        /*Deobfuscator.builder()
                .input(Path.of("example/paramorphism/Evaluator-obf.jar"))
                .output(Path.of("example/paramorphism/Evaluator-deobf.jar"))
                .transformers(
                        new ParamorphismStringTransformer(
                                "a/a/a/s/s",
                                "a/a/a/s/Dispatcher"
                        ),
                        new ParamorphismInvokeDynamicTransformer(
                                "a/a/a/b/m/⛔$0"
                        ),
                        new ParamorphismFlowTransformer(),
                        new ParamorphismKurwaNaChujTaKlasaRemoveTransformer()
                )
                .classReaderFlags(ClassReader.SKIP_FRAMES)
                .classWriterFlags(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES)
                .build()
                .start();*/

        Deobfuscator.builder()
                .input(Path.of("example/paramorphism/Evaluator-packed.jar"))
                .output(Path.of("example/paramorphism/Evaluator-unpacked.jar"))
                .transformers(
                        new ParamorphismPackerTransformer(),
                        new ParamorphismStringTransformer(
                                "a/a/a/s/s",
                                "a/a/a/s/Dispatcher"
                        ),
                        new ParamorphismInvokeDynamicTransformer(
                                "a/a/a/b/m/⛔$0"
                        ),
                        new ParamorphismFlowTransformer(),
                        new ParamorphismKurwaNaChujTaKlasaRemoveTransformer()
                )
                .classReaderFlags(ClassReader.SKIP_FRAMES)
                .classWriterFlags(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES)
                .build()
                .start();

    }
}
