import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.impl.binsecure.latest.BinsecureCrasherRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.binsecure.latest.SemiBinsecureMbaTransformer;

import java.nio.file.Path;

public class Loader {

    public static void main(String... args) throws Exception {
        Deobfuscator.builder()
                .input(Path.of("example/binsecure 1.8.3/Evaluator-obf.jar"))
                .output(Path.of("example/binsecure 1.8.3/Evaluator-deobf.jar"))
                .transformers(
                        new BinsecureCrasherRemoveTransformer(),
                        new SemiBinsecureMbaTransformer()
                )
                .classReaderFlags(0)
                .classWriterFlags(0)
                .build()
                .start();

    }
}
