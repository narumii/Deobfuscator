import org.objectweb.asm.ClassReader;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.impl.bozar.CleanTransformer;
import uwu.narumi.deobfuscator.transformer.impl.bozar.FlowRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.bozar.TypeLdcRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.bozar.WhileLoopRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UnHideTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.*;

import java.nio.file.Path;

public class Loader {

    public static void main(String... args) throws Exception {
        Deobfuscator.builder()
                .input(Path.of("example/Ayakashi.jar"))
                .output(Path.of("example/Ayakashi-deobf.jar"))
                .transformers(
                        new ImageCrasherRemoveTransformer(),
                        new InvalidAnnotationRemoveTransformer(),

                        new NopRemoveTransformer(),
                        new LineNumberRemoveTransformer(),
                        new LocalVariableRemoveTransformer(),
                        new TrashPopRemoveTransformer(),

                        new TypeLdcRemoveTransformer(),
                        new FlowRemoveTransformer(),
                        new WhileLoopRemoveTransformer(),
                        new CleanTransformer(),

                        new SignatureRemoveTransformer(),
                        new UnHideTransformer()
                )
                .classReaderFlags(ClassReader.SKIP_FRAMES)
                .build()
                .start();

    }
}
