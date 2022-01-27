import org.objectweb.asm.ClassReader;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.impl.cheatbreaker.*;
import uwu.narumi.deobfuscator.transformer.impl.sb27.*;
import uwu.narumi.deobfuscator.transformer.impl.sentinel.SentinelStringTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UniversalNumberTransformer;

import java.nio.file.Path;

public class Loader {

    public static void main(String... args) throws Exception {
        Deobfuscator.builder()
                .input(Path.of("..\\jd-gui-1.6.6.manipulated.jar"))
                .output(Path.of("..\\jd-gui-1.6.6.manipulated-deobf.jar"))
                .transformers(
                        new SentinelStringTransformer()
                )
                .classReaderFlags(ClassReader.SKIP_FRAMES)
                .classWriterFlags(0)
                .consoleDebug()
                .build()
                .start();

    }
}
