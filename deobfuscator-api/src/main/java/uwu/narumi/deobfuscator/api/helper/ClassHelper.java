package uwu.narumi.deobfuscator.api.helper;

import java.util.ArrayList;

import software.coley.cafedude.InvalidClassException;
import software.coley.cafedude.classfile.ClassFile;
import software.coley.cafedude.io.ClassFileReader;
import software.coley.cafedude.io.ClassFileWriter;
import org.intellij.lang.annotations.MagicConstant;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;

public final class ClassHelper {

  private ClassHelper() {
  }

  public static boolean isClass(String fileName, byte[] bytes) {
    return isClass(bytes) && (fileName.endsWith(".class") || fileName.endsWith(".class/"));
  }

  public static boolean isClass(byte[] bytes) {
    return bytes.length >= 4
        && String.format("%02X%02X%02X%02X", bytes[0], bytes[1], bytes[2], bytes[3]).equals("CAFEBABE");
  }

  /**
   * Load class from bytes
   *
   * @param pathInJar        Relative path of a class in a jar
   * @param bytes            Class bytes
   * @param classReaderFlags {@link ClassReader} flags
   */
  public static ClassWrapper loadClass(
      String pathInJar,
      byte[] bytes,
      @MagicConstant(flagsFromClass = ClassReader.class) int classReaderFlags
  ) {
    return new ClassWrapper(pathInJar, new ClassReader(bytes), classReaderFlags);
  }

  /**
   * Loads only class info (like class name, superclass, interfaces, etc.) without any code.
   *
   * @param bytes Class bytes
   * @return {@link ClassNode} with class info only
   */
  public static ClassNode loadClassInfo(byte[] bytes) {
    ClassNode classNode = new ClassNode();
    new ClassReader(bytes).accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
    return classNode;
  }

  /**
   * Fix class using CAFED00D
   *
   * @param bytes Class bytes
   * @return Fixed class bytes
   */
  public static byte[] fixClass(byte[] bytes) throws InvalidClassException {
    ClassFileReader classFileReader = new ClassFileReader();
    ClassFile classFile = classFileReader.read(bytes);
    bytes = new ClassFileWriter().write(classFile);

    return bytes;
  }

  public static ClassNode copy(ClassNode classNode) {
    if (classNode == null) return null;

    ClassNode copy = new ClassNode();
    copy.visit(
        classNode.version,
        classNode.access,
        classNode.name,
        null,
        classNode.superName,
        classNode.interfaces.toArray(new String[0]));
    classNode.accept(copy);

    copy.methods = new ArrayList<>();
    copy.fields = new ArrayList<>();

    classNode.methods.forEach(
        methodNode -> {
          MethodNode copyMethod =
              new MethodNode(
                  methodNode.access,
                  methodNode.name,
                  methodNode.desc,
                  null,
                  methodNode.exceptions.toArray(new String[0]));
          methodNode.accept(copyMethod);
          copy.methods.add(copyMethod);
        });

    classNode.fields.forEach(
        fieldNode ->
            copy.fields.add(
                new FieldNode(
                    fieldNode.access, fieldNode.name, fieldNode.desc, null, fieldNode.value)));

    return copy;
  }
}
