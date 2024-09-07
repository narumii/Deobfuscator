package reverseengineering.zelix.longdecrypter;

import java.lang.invoke.MethodHandles;

public class Main {
  // Usage
  private static final long result = LongDecrypter1.buildNumberDecryptor(5832394289974403481L, -8943439614781261032L, MethodHandles.lookup().lookupClass()).decrypt(19597665297729L);

  public static void main(String[] args) {
    System.out.println(result);
  }
}
