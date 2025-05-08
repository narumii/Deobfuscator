package uwu.narumi.deobfuscator.core.other.impl.universal;

import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;
import uwu.narumi.deobfuscator.api.asm.remapper.NamesRemapper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;

/**
 * Recovers class name from source file name.
 */
public class SourceFileNameRecoverTransformer extends Transformer {
  @Override
  protected void transform() throws Exception {
    NamesRemapper remapper = new NamesRemapper();

    scopedClasses().forEach(classWrapper -> {
      String sourceFileName = classWrapper.classNode().sourceFile;
      if (sourceFileName == null) return;

      // Replace the class name with the source file name without touching the package
      String newClassName = classWrapper.classNode().name.replaceAll("/[^/]+$", "/" + sourceFileName.replaceAll("\\.java$", ""));

      remapper.classMappings.put(classWrapper.name(), newClassName);

      markChange();
    });

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
    });
  }
}
