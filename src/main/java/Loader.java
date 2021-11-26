import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.impl.monsey.MonseyFakeTryCatchTransformer;
import uwu.narumi.deobfuscator.transformer.impl.qprotect.b31.qProtectStringTransformer;
import uwu.narumi.deobfuscator.transformer.impl.qprotect.latest.qProtectFieldFlowTransformer;
import uwu.narumi.deobfuscator.transformer.impl.qprotect.latest.qProtectFlowTransformer;
import uwu.narumi.deobfuscator.transformer.impl.qprotect.latest.qProtectInvokeDynamicTransformer;
import uwu.narumi.deobfuscator.transformer.impl.qprotect.latest.qProtectNumberTransformer;
import uwu.narumi.deobfuscator.transformer.impl.sb27.SuperblaubeereFlowTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.TryCatchFixTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.other.UnHideTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.SignatureRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.universal.remove.UnknownAttributeRemoveTransformer;

import java.nio.file.Path;

public class Loader {

    public static void main(String... args) throws Exception {
        Deobfuscator.builder()
                .input(Path.of("example\\qprotect\\latest\\CrackMeFULL.jar"))
                .output(Path.of("example\\qprotect\\latest\\CrackMeFULL-deobf.jar"))
                .transformers(
                        new qProtectNumberTransformer(),
                        new qProtectFieldFlowTransformer(),
                        new MonseyFakeTryCatchTransformer(),
                        new qProtectFlowTransformer(),
                        new qProtectNumberTransformer(),
                        new SuperblaubeereFlowTransformer(),
                        new qProtectFlowTransformer(),
                        new qProtectStringTransformer(),
                        new TryCatchFixTransformer(),
                        new qProtectInvokeDynamicTransformer(),
                        new uwu.narumi.deobfuscator.transformer.impl.qprotect.latest.qProtectStringTransformer(),
                        new UnknownAttributeRemoveTransformer(),
                        new SignatureRemoveTransformer(),
                        new UnHideTransformer()
                )
                .classReaderFlags(ClassReader.SKIP_FRAMES)
                .classWriterFlags(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES)
                .consoleDebug()
                .build()
                .start();

    }
}
