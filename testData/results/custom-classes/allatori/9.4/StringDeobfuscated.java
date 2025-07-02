import java.io.File;

// Procyon
public class Obfuscate {
  public static /* synthetic */ void dummy() {
    System.out.println(/*EL:295*/new StringBuilder().insert(0x3 & 0x4, a.b()).append(" ").append(c.d()).toString());
    System.out.println(/*EL:338*/"Usage:");
    System.out.println(/*EL:580*/"Dummy string!");
  }
}
