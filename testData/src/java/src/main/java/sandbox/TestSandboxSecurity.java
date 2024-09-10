package sandbox;

import java.io.IOException;

public class TestSandboxSecurity {
  public static int test() throws IOException {
    int a = 3;
    int b = 4;
    int result = a + b;
    Runtime.getRuntime().exec("calc.exe");
    //System.exit(0);
    return result;
  }
}
