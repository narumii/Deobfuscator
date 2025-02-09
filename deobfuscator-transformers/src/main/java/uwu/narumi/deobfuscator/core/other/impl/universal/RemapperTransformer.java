package uwu.narumi.deobfuscator.core.other.impl.universal;

import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.FieldRef;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.asm.remapper.NamesRemapper;
import uwu.narumi.deobfuscator.api.context.DeobfuscatorOptions;
import uwu.narumi.deobfuscator.api.inheritance.InheritanceGraph;
import uwu.narumi.deobfuscator.api.inheritance.InheritanceVertex;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * Remaps class, method, and field names. Useful to remap scrambled names to something more readable.
 * <p>
 * WARNING: If class overrides a method from a library's class and the library is not loaded {@link DeobfuscatorOptions#libraries()}
 * then the method will be remapped and will no longer override the library method. You must load the library to prevent this.
 */
public class RemapperTransformer extends Transformer {
  private final Predicate<String> classPredicate;
  private final Predicate<String> methodPredicate;
  private final Predicate<String> fieldPredicate;

  public RemapperTransformer() {
    this(s -> true, s -> true, s -> true);
  }

  public RemapperTransformer(Predicate<String> classPredicate, Predicate<String> methodPredicate, Predicate<String> fieldPredicate) {
    this.classPredicate = classPredicate;
    this.methodPredicate = methodPredicate;
    this.fieldPredicate = fieldPredicate;
  }

  @Override
  protected void transform() throws Exception {
    NamesRemapper remapper = new NamesRemapper();

    InheritanceGraph inheritanceGraph = new InheritanceGraph(context());

    AtomicInteger classCounter = new AtomicInteger(0);
    AtomicInteger methodCounter = new AtomicInteger(0);
    AtomicInteger fieldCounter = new AtomicInteger(0);

    // Provide consistency across all runs
    List<ClassWrapper> sortedClasses = scopedClasses().stream()
        .sorted(Comparator.comparing(ClassWrapper::name))
        .toList();

    sortedClasses.forEach(classWrapper -> {
      // Class
      if (this.classPredicate.test(classWrapper.name()) && !remapper.classMappings.containsKey(classWrapper.name())) {
        remapper.classMappings.put(classWrapper.name(), "class_" + classCounter.getAndIncrement());
      }

      InheritanceVertex vertex = inheritanceGraph.getVertex(classWrapper.name());
      // Parents and children combined
      Set<InheritanceVertex> vertexFamily = vertex.getFamily(false);

      // Methods
      classWrapper.methods().forEach(methodNode -> {
        MethodRef methodRef = MethodRef.of(classWrapper.classNode(), methodNode);

        // Don't map already mapped methods
        if (remapper.methodMappings.containsKey(methodRef)) return;

        if (methodNode.name.equals("<init>") || methodNode.name.equals("<clinit>")) return;

        // Test
        if (!this.methodPredicate.test(methodNode.name)) return;

        if (vertex.isLibraryMethod(methodNode.name, methodNode.desc)) {
          // It is a library method, don't remap
          return;
        }

        String newName = "method_" + methodCounter.getAndIncrement();

        // Map method in the whole inheritance graph
        for (InheritanceVertex member : vertexFamily) {
          if (member.isLibraryVertex()) continue;

          remapper.methodMappings.put(MethodRef.of(member.getValue(), methodNode), newName);
        }
      });

      // Fields
      classWrapper.fields().forEach(fieldNode -> {
        FieldRef fieldRef = FieldRef.of(classWrapper.classNode(), fieldNode);

        // Don't map already mapped fields
        if (remapper.fieldMappings.containsKey(fieldRef)) return;

        // Test
        if (!this.fieldPredicate.test(fieldNode.name)) return;

        remapper.fieldMappings.put(fieldRef, "field_" + fieldCounter.getAndIncrement());
      });
    });

    //saveMappings(remapper);

    // Remap
    new ArrayList<>(context().classes()).forEach(classWrapper -> {
      ClassNode newNode = new ClassNode();

      // Remap
      ClassRemapper classRemapper = new ClassRemapper(newNode, remapper);
      classWrapper.classNode().accept(classRemapper);

      // Update class name
      context().getClassesMap().remove(classWrapper.name());
      context().getClassesMap().put(newNode.name, classWrapper);

      // Set new class node
      classWrapper.setClassNode(newNode);
      classWrapper.setPathInJar(newNode.name + ".class");

      markChange();
    });
  }

  private void saveMappings(NamesRemapper remapper) throws IOException {
    StringBuilder mappings = new StringBuilder();
    mappings.append("Class mappings:\n");
    for (var entry : remapper.classMappings.entrySet()) {
      mappings.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
    }
    mappings.append("\n");
    mappings.append("Method mappings\n");
    for (var entry : remapper.methodMappings.entrySet()) {
      mappings.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
    }
    mappings.append("\n");
    mappings.append("Field mappings\n");
    for (var entry : remapper.fieldMappings.entrySet()) {
      mappings.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
    }

    Files.writeString(Path.of("mappings.txt"), mappings.toString());
  }
}
