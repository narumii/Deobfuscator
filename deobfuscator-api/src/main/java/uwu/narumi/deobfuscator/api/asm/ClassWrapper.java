package uwu.narumi.deobfuscator.api.asm;

import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.helper.ClassHelper;

public class ClassWrapper implements Cloneable {

  protected static final Logger LOGGER = LogManager.getLogger(ClassWrapper.class);

  private ClassNode classNode;
  private FieldCache fieldCache;

  public ClassWrapper(ClassNode classNode) {
    this.classNode = classNode;
    this.fieldCache = new FieldCache();
  }

  public Optional<MethodNode> findMethod(String name, String desc) {
    return classNode.methods.stream()
        .filter(methodNode -> name == null || methodNode.name.equals(name))
        .filter(methodNode -> desc == null || methodNode.desc.equals(desc))
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

  public List<FieldNode> fields() {
    return classNode.fields;
  }

  public List<MethodNode> methods() {
    return classNode.methods;
  }

  public ClassNode getClassNode() {
    return classNode;
  }

  public FieldCache getFieldCache() {
    return fieldCache;
  }

  @Override
  public ClassWrapper clone() {
    ClassWrapper classWrapper = new ClassWrapper(ClassHelper.copy(classNode));
    classWrapper.fieldCache = this.fieldCache.clone();
    return classWrapper;
  }
}
