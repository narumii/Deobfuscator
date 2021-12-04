import org.objectweb.asm.ClassReader;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.impl.qprotect.latest.qProtectFieldFlowTransformer;
import uwu.narumi.deobfuscator.transformer.impl.scuti.ScutiInvokeDynamicTransformer;
import uwu.narumi.deobfuscator.transformer.impl.scuti.ScutiNormalStringTransformer;
import uwu.narumi.deobfuscator.transformer.impl.scuti.ScutiStrongStringTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.StackOperationTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.TryCatchFixTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UniversalNumberTransformer;

import java.nio.file.Path;

public class Loader {

    public static void main(String... args) throws Exception {
        Deobfuscator.builder()
                .input(Path.of("example\\scuti\\Evaluator-strong.jar"))
                .output(Path.of("example\\scuti\\Evaluator-strong-deobf.jar"))
                .transformers(
                        new StackOperationTransformer(),
                        new UniversalNumberTransformer(),
                        new qProtectFieldFlowTransformer(),
                        new ScutiInvokeDynamicTransformer(),
                        new StackOperationTransformer(),
                        new UniversalNumberTransformer(),

                        new ScutiStrongStringTransformer(), // https://github.com/netindev/scuti/blob/467b856b7ea46009608ccdf4db69b4b43e640fa6/scuti-core/src/main/java/tk/netindev/scuti/core/transform/obfuscation/StringEncryptionTransformer.java#L188
                        new ScutiNormalStringTransformer(), // https://github.com/netindev/scuti/blob/467b856b7ea46009608ccdf4db69b4b43e640fa6/scuti-core/src/main/java/tk/netindev/scuti/core/transform/obfuscation/StringEncryptionTransformer.java#L148

                        new TryCatchFixTransformer()

                )
                .classReaderFlags(ClassReader.SKIP_FRAMES)
                .classWriterFlags(0)
                .consoleDebug()
                .build()
                .start();

    }
}
