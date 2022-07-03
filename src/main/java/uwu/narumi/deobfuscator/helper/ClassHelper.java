package uwu.narumi.deobfuscator.helper;

import me.coley.cafedude.classfile.ClassFile;
import me.coley.cafedude.InvalidClassException;
import me.coley.cafedude.io.ClassFileReader;
import me.coley.cafedude.io.ClassFileWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;

public final class ClassHelper {

    private ClassHelper() {
    }

    public static boolean isClass(String fileName, byte[] bytes) {
        return bytes.length >= 4 && String
                .format("%02X%02X%02X%02X", bytes[0], bytes[1], bytes[2], bytes[3]).equals("CAFEBABE") && (
                fileName.endsWith(".class") || fileName.endsWith(".class/"));
    }

    public static ClassNode loadClass(byte[] bytes, int readerMode) throws InvalidClassException {
        return loadClass(bytes, readerMode, true);
    }

    public static ClassNode loadClass(byte[] bytes, int readerMode, boolean fix) throws InvalidClassException {
        ClassNode classNode;
        try {
            classNode = new ClassNode();
            ClassReader classReader = new ClassReader(bytes);
            classReader.accept(classNode, readerMode);
        } catch (Exception e) {
            classNode = fix ? loadClass(fixClass(bytes), readerMode, false) : null;
        }

        return classNode;
    }

    public static byte[] fixClass(byte[] bytes) throws InvalidClassException {
        ClassFileReader classFileReader = new ClassFileReader();
        ClassFile classFile = classFileReader.read(bytes);
        bytes = new ClassFileWriter().write(classFile);

        return bytes;
    }

    public static byte[] classToBytes(ClassNode classNode, int writerMode) {
        ClassWriter classWriter = new ClassWriter(writerMode);
        classNode.accept(classWriter);

        return classWriter.toByteArray();
    }

    public static ClassNode copy(ClassNode classNode) {
        if (classNode == null)
            return null;

        ClassNode copy = new ClassNode();
        copy.visit(classNode.version, classNode.access, classNode.name, null, classNode.superName, classNode.interfaces.toArray(new String[0]));
        classNode.accept(copy);

        copy.methods = new ArrayList<>();
        copy.fields = new ArrayList<>();

        classNode.methods.forEach(methodNode -> {
            MethodNode copyMethod = new MethodNode(methodNode.access, methodNode.name, methodNode.desc, null, methodNode.exceptions.toArray(new String[0]));
            methodNode.accept(copyMethod);
            copy.methods.add(copyMethod);
        });

        classNode.fields.forEach(fieldNode -> {
            copy.fields.add(new FieldNode(fieldNode.access, fieldNode.name, fieldNode.desc, null, fieldNode.value));
        });

        return copy;
    }
}

