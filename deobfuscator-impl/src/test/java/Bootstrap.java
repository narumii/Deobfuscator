import dev.xdark.ssvm.VirtualMachine;
import java.nio.file.Path;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.Deobfuscator;

public class Bootstrap {

  public static void main(String[] args) throws Exception {
    Deobfuscator.builder()
        .inputJar(Path.of("work", "obf-test.jar"))
        .virtualMachine(
            new VirtualMachine() {
              // you can do shit
            })
        .transformers()
        .consoleDebug()
        .classReaderFlags(ClassReader.SKIP_FRAMES)
        .classWriterFlags(ClassWriter.COMPUTE_FRAMES)
        .build()
        .start();
  }
}
