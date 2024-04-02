package uwu.narumi.deobfuscator.api.asm;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;

public class FieldCache implements Cloneable {

  private final Map<FieldInfo, Object> values;

  public FieldCache() {
    this(new HashMap<>());
  }

  private FieldCache(Map<FieldInfo, Object> values) {
    this.values = values;
  }

  public <T> T getOrCompute(FieldInfo key, T value) {
    if (!isPresent(key)) values.put(key, value);

    return (T) values.get(key);
  }

  public <T> T getOrCompute(FieldNode fieldNode, T value) {
    return getOrCompute(FieldInfo.of(fieldNode), value);
  }

  public <T> T getOrCompute(FieldInsnNode FieldInsnNode, T value) {
    return getOrCompute(FieldInfo.of(FieldInsnNode), value);
  }

  public <T> T getOrCompute(String name, String desc, T value) {
    return getOrCompute(FieldInfo.of(name, desc), value);
  }

  public <T> T get(FieldInfo key) {
    return (T) values.get(key);
  }

  public <T> T get(FieldNode fieldNode) {
    return get(FieldInfo.of(fieldNode));
  }

  public <T> T get(FieldInsnNode fieldInsnNode) {
    return get(FieldInfo.of(fieldInsnNode));
  }

  public <T> T get(String name, String desc) {
    return get(FieldInfo.of(name, desc));
  }

  public <T> T set(FieldInfo key, T value) {
    return (T) values.put(key, value);
  }

  public <T> T set(FieldNode fieldNode, T value) {
    return set(FieldInfo.of(fieldNode), value);
  }

  public <T> T set(FieldInsnNode fieldInsnNode, T value) {
    return set(FieldInfo.of(fieldInsnNode), value);
  }

  public <T> T set(String name, String desc, T value) {
    return set(FieldInfo.of(name, desc), value);
  }

  public boolean isPresent(FieldInfo key) {
    return values.get(key) != null;
  }

  public boolean isPresent(FieldNode fieldNode) {
    return isPresent(FieldInfo.of(fieldNode));
  }

  public boolean isPresent(FieldInsnNode fieldInsnNode) {
    return isPresent(FieldInfo.of(fieldInsnNode));
  }

  public boolean isPresent(String name, String desc) {
    return isPresent(FieldInfo.of(name, desc));
  }

  public boolean has(FieldInfo fieldInfo) {
    return values.containsKey(fieldInfo);
  }

  public boolean has(FieldNode fieldNode) {
    return has(FieldInfo.of(fieldNode));
  }

  public boolean has(FieldInsnNode fieldInsnNode) {
    return has(FieldInfo.of(fieldInsnNode));
  }

  public boolean has(String name, String desc) {
    return has(FieldInfo.of(name, desc));
  }

  @Override
  protected FieldCache clone() {
    return new FieldCache(new HashMap<>(Map.copyOf(values)));
  }

  public record FieldInfo(String name, String desc) {
    public static FieldInfo of(String name, String desc) {
      return new FieldInfo(name, desc);
    }

    public static FieldInfo of(FieldInsnNode fieldInsnNode) {
      return new FieldInfo(fieldInsnNode.name, fieldInsnNode.desc);
    }

    public static FieldInfo of(FieldNode fieldNode) {
      return new FieldInfo(fieldNode.name, fieldNode.desc);
    }
  }
}
