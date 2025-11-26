import java.nio.file.Path;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.api.context.DeobfuscatorOptions;
import uwu.narumi.deobfuscator.core.other.composed.ComposedGruntTransformer;

public class Bootstrap {

  public static void main(String[] args) {
    Deobfuscator.from(
        DeobfuscatorOptions.builder()
            .inputJar(Path.of("work", "grunt-more.jar")) // Specify your input jar here
            //.libraries(Path.of("work", "libs")) // Specify your libraries here if needed
            .transformers(
                ComposedGruntTransformer::new
            )
            .continueOnError()
            .classWriterFlags(ClassWriter.COMPUTE_FRAMES)
            .build()
    ).start();
  }
}
