package uwu.narumi.deobfuscator.api.asm;

import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.classpath.InheritanceClassWriter;
import uwu.narumi.deobfuscator.api.inheritance.InheritanceGraph;

public class ClassWrapper {

  protected static final Logger LOGGER = LogManager.getLogger(ClassWrapper.class);

  /**
   * Relative path inside jar. Mainly for saving purposes.
   */
  private String pathInJar;
  private ClassNode classNode;
  private final ConstantPool constantPool;

  public ClassWrapper(String pathInJar, ClassReader classReader, int classReaderFlags) {
    this.pathInJar = pathInJar;
    this.classNode = new ClassNode();
    this.constantPool = new ConstantPool(classReader);

    classReader.accept(this.classNode, classReaderFlags);
  }

  public Optional<MethodNode> findMethod(String name, String desc) {
    return classNode.methods.stream()
        .filter(methodNode -> name == null || methodNode.name.equals(name))
        .filter(methodNode -> desc == null || methodNode.desc.equals(desc))
        .findFirst();
  }

  public Optional<MethodNode> findMethod(MethodRef methodRef) {
    if (!methodRef.owner().equals(classNode.name)) {
      return Optional.empty();
    }
    return classNode.methods.stream()
        .filter(methodNode -> methodNode.name.equals(methodRef.name()) && methodNode.desc.equals(methodRef.desc()))
        .findFirst();
  }

  public Optional<MethodNode> findMethod(String name, Class<?> returnType, Class<?>... parameters) {
    String descriptor = MethodType.methodType(returnType, parameters).toMethodDescriptorString();
    return classNode.methods.stream()
        .filter(methodNode -> name == null || methodNode.name.equals(name))
        .filter(methodNode -> descriptor.equals(methodNode.desc))
        .findFirst();
  }

  public Optional<MethodNode> findMethod(String name, Class<?>... parameters) {
    return findMethod(name, Void.TYPE, parameters);
  }

  public Optional<MethodNode> findMethod(Predicate<String> name, Predicate<String> desc) {
    return classNode.methods.stream()
        .filter(methodNode -> name == null || name.test(methodNode.name))
        .filter(methodNode -> desc == null || desc.test(methodNode.desc))
        .findFirst();
  }

  public Optional<MethodNode> findMethod(Predicate<MethodNode> predicate) {
    return classNode.methods.stream().filter(predicate).findFirst();
  }

  public Optional<MethodNode> findMethod(MethodInsnNode methodInsnNode) {
    return classNode.methods.stream()
        .filter(methodNode -> methodNode.name.equals(methodInsnNode.name))
        .filter(methodNode -> methodNode.desc.equals(methodInsnNode.desc))
        .findFirst();
  }

  public Optional<FieldNode> findField(String name, Class<?> type) {
    return classNode.fields.stream()
        .filter(fieldNode -> name == null || fieldNode.name.equals(name))
        .filter(fieldNode -> Type.getType(type).getDescriptor().equals(fieldNode.desc))
        .findFirst();
  }

  public Optional<FieldNode> findField(String name, String desc) {
    return classNode.fields.stream()
        .filter(fieldNode -> name == null || fieldNode.name.equals(name))
        .filter(fieldNode -> desc == null || fieldNode.desc.equals(desc))
        .findFirst();
  }

  public Optional<FieldNode> findField(Predicate<String> name, Predicate<String> desc) {
    return classNode.fields.stream()
        .filter(fieldNode -> name == null || name.test(fieldNode.name))
        .filter(fieldNode -> desc == null || desc.test(fieldNode.desc))
        .findFirst();
  }

  public Optional<FieldNode> findField(Predicate<FieldNode> predicate) {
    return classNode.fields.stream().filter(predicate).findFirst();
  }

  public Optional<FieldNode> findField(FieldInsnNode fieldInsnNode) {
    return classNode.fields.stream()
        .filter(fieldNode -> fieldNode.name.equals(fieldInsnNode.name))
        .filter(fieldNode -> fieldNode.desc.equals(fieldInsnNode.desc))
        .findFirst();
  }

  public Optional<MethodNode> findClInit() {
    return findMethod("<clinit>", "()V");
  }

  public String name() {
    return classNode.name;
  }

  public String canonicalName() {
    return classNode.name.replace('/', '.');
  }

  /**
   * Compiles class to bytes.
   */
  public byte[] compileToBytes(InheritanceGraph inheritanceGraph, int classWriterFlags) {
    try {
      ClassWriter classWriter = new InheritanceClassWriter(classWriterFlags, inheritanceGraph);
      this.classNode.accept(classWriter);

      return classWriter.toByteArray();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  public String getPathInJar() {
    return pathInJar;
  }

  public List<FieldNode> fields() {
    return classNode.fields;
  }

  public List<MethodNode> methods() {
    return classNode.methods;
  }

  public ClassNode classNode() {
    return classNode;
  }

  @ApiStatus.Internal
  public void setClassNode(ClassNode classNode) {
    this.classNode = classNode;
  }

  @ApiStatus.Internal
  public void setPathInJar(String pathInJar) {
    this.pathInJar = pathInJar;
  }

  public ConstantPool getConstantPool() {
    return constantPool;
  }
}
