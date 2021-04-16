package uwu.narumi.deobfuscator.helper;

import org.objectweb.asm.ClassReader;
import uwu.narumi.deobfuscator.pool.ConstantPool;

public class ClassHelper {

  public static boolean isClass(String fileName, byte[] bytes) {
    return bytes.length >= 4 && String
        .format("%02X%02X%02X%02X", bytes[0], bytes[1], bytes[2], bytes[3]).equals("CAFEBABE") && (
        fileName.endsWith(".class") || fileName.endsWith(".class/"));
  }

  public static ConstantPool getConstantPool(ClassReader classReader) {
    int[] ints = new int[classReader.getItemCount()];
    for (int i = 0; i < ints.length; i++) {
      ints[i] = classReader.getItem(i);
    }

    return new ConstantPool(ints);
  }
}
