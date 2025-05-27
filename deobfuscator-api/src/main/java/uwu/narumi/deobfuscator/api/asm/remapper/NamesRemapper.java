package uwu.narumi.deobfuscator.api.asm.remapper;

import org.objectweb.asm.commons.Remapper;
import uwu.narumi.deobfuscator.api.asm.FieldRef;
import uwu.narumi.deobfuscator.api.asm.MethodRef;

import java.util.HashMap;
import java.util.Map;

public class NamesRemapper extends Remapper {
  public final Map<String, String> classMappings = new HashMap<>(); // old class internal name -> new class internal name
  public final Map<MethodRef, String> methodMappings = new HashMap<>(); // old method name -> new method name
  public final Map<FieldRef, String> fieldMappings = new HashMap<>(); // old field name -> new field name

  @Override
  public String mapMethodName(String owner, String name, String descriptor) {
    String newName = this.methodMappings.get(new MethodRef(owner, name, descriptor));
    return newName != null ? newName : name;
  }

  @Override
  public String mapFieldName(String owner, String name, String descriptor) {
    String newName = this.fieldMappings.get(new FieldRef(owner, name, descriptor));
    return newName != null ? newName : name;
  }

  @Override
  public String map(String internalName) {
    String newName = this.classMappings.get(internalName);
    return newName != null ? newName : internalName;
  }
}
