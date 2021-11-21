import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.impl.hp888.StaticArrayIntegerPoolTransformer;
import uwu.narumi.deobfuscator.transformer.impl.hp888.StaticArrayLongPoolTransformer;
import uwu.narumi.deobfuscator.transformer.impl.hp888.StaticArrayStringPoolTransformer;
import uwu.narumi.deobfuscator.transformer.impl.rakszild.RakSzildStringTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UniversalNumberTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.LocalVariableRemoveTransformer;

import java.nio.file.Path;

public class Loader {

    public static void main(String... args) throws Exception {
        Deobfuscator.builder()
                .input(Path.of("example/V1_KIT-obf_4_2.jar"))
                .output(Path.of("example/V1_KIT-obf_4_2-deobf.jar"))
                .transformers(
                        new UniversalNumberTransformer(),
                        new StaticArrayIntegerPoolTransformer(),
                        new StaticArrayLongPoolTransformer(),
                        new RakSzildStringTransformer(),
                        new StaticArrayStringPoolTransformer(),
                        new LocalVariableRemoveTransformer()
                )
                .classReaderFlags(ClassReader.SKIP_FRAMES)
                .classWriterFlags(ClassWriter.COMPUTE_MAXS)
                .consoleDebug()
                .build()
                .start();

    }
}
