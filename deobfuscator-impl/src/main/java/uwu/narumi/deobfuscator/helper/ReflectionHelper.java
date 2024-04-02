package uwu.narumi.deobfuscator.helper;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

public final class ReflectionHelper {

  private static final Unsafe unsafe;

  static {
    try {
      Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
      theUnsafe.setAccessible(true);
      unsafe = (Unsafe) theUnsafe.get(null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private ReflectionHelper() {}

  public static Unsafe getUnsafe() {
    return unsafe;
  }
}
