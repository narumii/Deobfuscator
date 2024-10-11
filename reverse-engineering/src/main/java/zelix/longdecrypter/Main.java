package zelix.longdecrypter;

import java.lang.invoke.MethodHandles;

public class Main {
  // Usage

  public static void main(String[] args) {
    // decrypterA must be decrypted first for decrypterB to be decrypted correctly. So there is specific decryption order:
    // decrypterA -> decrypterB
    ILongDecrypter decrypterA = SimpleLongDecrypter.buildNumberDecryptor(273921918217628048L, -8431841081763909460L, MethodHandles.lookup().lookupClass());
    System.out.println(decrypterA);
    long a = decrypterA.decrypt(36730249601911L);
    System.out.println(a);

    ILongDecrypter decrypterB = SimpleLongDecrypter.buildNumberDecryptor(-5385547845782035026L, 2563870759759473543L, MethodHandles.lookup().lookupClass());
    System.out.println(decrypterB);
    long b = decrypterB.decrypt(130571778689313L);
    System.out.println(b);
  }
}
