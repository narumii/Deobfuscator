import java.nio.file.Path;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.api.context.DeobfuscatorOptions;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedGeneralFlowTransformer;

public class Bootstrap {

  public static void main(String[] args) {
    Deobfuscator.from(
        DeobfuscatorOptions.builder()
            .inputJar(Path.of("work", "obf-test.jar"))
            .transformers(
                // Pick your transformers here
                () -> new ComposedGeneralFlowTransformer()
            )
            .continueOnError()
            .classReaderFlags(ClassReader.SKIP_FRAMES)
            .classWriterFlags(ClassWriter.COMPUTE_FRAMES)
            .build()
        ).start();
  }
}
