import org.objectweb.asm.ClassReader;
import uwu.narumi.deobfuscator.Deobfuscator;

import java.nio.file.Path;

public class Loader {

    public static void main(String... args) throws Exception {
        Deobfuscator.builder()
                .input(Path.of(""))
                .output(Path.of(""))
                .transformers(

                )
                .classReaderFlags(ClassReader.SKIP_FRAMES)
                .classWriterFlags(0)
                .consoleDebug()
                .build()
                .start();

    }
}
