import org.objectweb.asm.ClassReader;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.impl.cheatbreaker.CheatBreakerJunkFieldRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.cheatbreaker.CheatBreakerStaticArrayStringPoolTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UniversalNumberTransformer;

import java.nio.file.Path;

public class Loader {

    public static void main(String... args) throws Exception {
        Deobfuscator.builder()
                .input(Path.of("test", "Evaluator-cheatbreaker.jar"))
                .output(Path.of("test", "Evaluator-cheatbreaker-deobf.jar"))
                .transformers(
                        new UniversalNumberTransformer(),
                        new CheatBreakerJunkFieldRemoveTransformer(),
                        new CheatBreakerStaticArrayStringPoolTransformer()
                )
                .classReaderFlags(ClassReader.SKIP_FRAMES)
                .classWriterFlags(0)
                .consoleDebug()
                .build()
                .start();

    }
}
