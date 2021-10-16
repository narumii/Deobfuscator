import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.impl.binsecure.latest.SemiBinsecureMbaTransformer;
import uwu.narumi.deobfuscator.transformer.impl.mosey.MoseyFakeJumpTransformer;
import uwu.narumi.deobfuscator.transformer.impl.mosey.MoseyFakeTryCatchTransformer;
import uwu.narumi.deobfuscator.transformer.impl.mosey.MoseyStringTransformer;
import uwu.narumi.deobfuscator.transformer.impl.sb27.SuperblaubeereFlowTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.RefreshTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.TryCatchFixTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UnHideTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.InvalidAnnotationRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.NopRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.OkThrowRemoveTransformer;

import java.nio.file.Path;

public class Loader {

    public static void main(String... args) throws Exception {
        Deobfuscator.builder()
                .input(Path.of("example/monsey/Evaluator-obf.jar"))
                .output(Path.of("example/monsey/Evaluator-deobf.jar"))
                .transformers( //"BadAnnotation", "SyntheticBridge", "ReverseJump", "FakeTryCatches", "FakeJump", "StringEncryption", "ClassEntryHider", "BadAttribute"
                        new MoseyStringTransformer(),
                        new MoseyFakeJumpTransformer(),
                        new MoseyFakeTryCatchTransformer(),
                        new OkThrowRemoveTransformer(),
                        new NopRemoveTransformer(),
                        new UnHideTransformer(),
                        new InvalidAnnotationRemoveTransformer()
                )
                .classReaderFlags(ClassReader.SKIP_FRAMES)
                .classWriterFlags(ClassWriter.COMPUTE_MAXS)
                .consoleDebug()
                .build()
                .start();

    }
}
