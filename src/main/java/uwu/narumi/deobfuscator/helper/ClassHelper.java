package uwu.narumi.deobfuscator.helper;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public final class ClassHelper {

    private ClassHelper() {
    }

    public static boolean isClass(String fileName, byte[] bytes) {
        return bytes.length >= 4 && String
                .format("%02X%02X%02X%02X", bytes[0], bytes[1], bytes[2], bytes[3]).equals("CAFEBABE") && (
                fileName.endsWith(".class") || fileName.endsWith(".class/"));
    }

    public static ClassNode loadClass(byte[] bytes, int readerMode) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, readerMode);

        return classNode;
    }

    public static byte[] classToBytes(ClassNode classNode, int writerMode) {
        ClassWriter classWriter = new ClassWriter(writerMode);
        classNode.accept(classWriter);

        return classWriter.toByteArray();
    }
}

