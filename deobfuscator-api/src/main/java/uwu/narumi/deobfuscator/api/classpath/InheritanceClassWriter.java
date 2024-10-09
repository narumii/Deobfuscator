package uwu.narumi.deobfuscator.api.classpath;

import org.objectweb.asm.ClassWriter;
import uwu.narumi.deobfuscator.api.inheritance.InheritanceGraph;

/**
 * A {@link ClassWriter} that uses a {@link InheritanceGraph} to determine the common superclass of two classes.
 */
public class InheritanceClassWriter extends ClassWriter {
  private final InheritanceGraph inheritanceGraph;

  public InheritanceClassWriter(int flags, InheritanceGraph inheritanceGraph) {
    super(flags);
    this.inheritanceGraph = inheritanceGraph;
  }

  @Override
  protected String getCommonSuperClass(String first, String second) {
    return this.inheritanceGraph.getCommon(first, second);
  }
}
