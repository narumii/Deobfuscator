import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.impl.caesium.*;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.TryCatchFixTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UniversalNumberTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.ImageCrasherRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.InvalidAnnotationRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.NopRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.TrashPopRemoveTransformer;

import java.nio.file.Path;

public class Loader {

    public static void main(String... args) throws Exception {
        Deobfuscator.builder()
                .input(Path.of("example/caesium/Evaluator-normal.jar"))
                .output(Path.of("example/caesium/Evaluator-normal-deobf.jar"))
                .transformers( //Oh fuck this is big
                        new ImageCrasherRemoveTransformer(),
                        new InvalidAnnotationRemoveTransformer(),
                        new TrashPopRemoveTransformer(),
                        new NopRemoveTransformer(),
                        new UniversalNumberTransformer(),
                        new CaesiumNumberTransformer(),
                        new CaesiumInvokeDynamicTransformer(),
                        new CaesiumFlowTransformer(),
                        new TryCatchFixTransformer(),
                        new CaesiumNumberTransformer(),
                        new CaesiumNumberPoolTransformer(),
                        new UniversalNumberTransformer(),
                        new CaesiumStringTransformer(),
                        new CaesiumCleanTransformer()
                )
                .classReaderFlags(ClassReader.SKIP_FRAMES)
                .classWriterFlags(ClassWriter.COMPUTE_MAXS)
                .build()
                .start();

    }
}
