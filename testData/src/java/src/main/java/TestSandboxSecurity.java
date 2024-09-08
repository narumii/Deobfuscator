import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestSandboxSecurity {
  public static int test() {
    int a = 3;
    int b = 4;
    int result = a + b;
    try {
      Files.createFile(Path.of("test.txt"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return result;
  }
}
