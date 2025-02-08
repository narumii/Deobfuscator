package uwu.narumi.deobfuscator.api.inheritance;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Graph element for a class inheritance hierarchy.
 *
 * @author Matt Coley
 */
// Copied from https://github.com/Col-E/Recaf/blob/ac6e07cbaf168a1f2093e71a39215bda8a00402d/recaf-core/src/main/java/software/coley/recaf/services/inheritance/InheritanceVertex.java
public class InheritanceVertex {
  private final Function<String, InheritanceVertex> lookup;
  private final Function<String, Collection<String>> childrenLookup;
  private final boolean isPrimary;
  private volatile Set<InheritanceVertex> parents;
  private volatile Set<InheritanceVertex> children;
  private ClassNode value;

  /**
   * @param value
   * 		The wrapped value.
   * @param lookup
   * 		Class vertex lookup.
   * @param childrenLookup
   * 		Class child lookup.
   */
  public InheritanceVertex(@NotNull ClassNode value,
                           @NotNull Function<String, InheritanceVertex> lookup,
                           @NotNull Function<String, Collection<String>> childrenLookup,
                           boolean isPrimary) {
    this.value = value;
    this.lookup = lookup;
    this.childrenLookup = childrenLookup;
    this.isPrimary = isPrimary;
  }

  /**
   * @param name
   * 		Field name.
   * @param desc
   * 		Field descriptor.
   *
   * @return If the field exists in the current vertex.
   */
  public boolean hasField(@NotNull String name, @NotNull String desc) {
    for (FieldNode fn : value.fields)
      if (fn.name.equals(name) && fn.desc.equals(desc))
        return true;
    return false;
  }

  /**
   * @param name
   * 		Field name.
   * @param desc
   * 		Field descriptor.
   *
   * @return If the field exists in the current vertex or in any parent vertex.
   */
  public boolean hasFieldInSelfOrParents(@NotNull String name, @NotNull String desc) {
    if (hasField(name, desc))
      return true;
    return allParents()
        .filter(v -> v != this)
        .anyMatch(parent -> parent.hasFieldInSelfOrParents(name, desc));
  }

  /**
   * @param name
   * 		Field name.
   * @param desc
   * 		Field descriptor.
   *
   * @return If the field exists in the current vertex or in any child vertex.
   */
  public boolean hasFieldInSelfOrChildren(@NotNull String name, @NotNull String desc) {
    if (hasField(name, desc))
      return true;
    return allChildren()
        .filter(v -> v != this)
        .anyMatch(parent -> parent.hasFieldInSelfOrChildren(name, desc));
  }

  /**
   * @param name
   * 		Method name.
   * @param desc
   * 		Method descriptor.
   *
   * @return If the method exists in the current vertex.
   */
  public boolean hasMethod(@NotNull String name, @NotNull String desc) {
    for (MethodNode mn : value.methods)
      if (mn.name.equals(name) && mn.desc.equals(desc))
        return true;
    return false;
  }

  /**
   * @param name
   * 		Method name.
   * @param desc
   * 		Method descriptor.
   *
   * @return If the method exists in the current vertex or in any parent vertex.
   */
  public boolean hasMethodInSelfOrParents(@NotNull String name, @NotNull String desc) {
    if (hasMethod(name, desc))
      return true;
    return allParents()
        .filter(v -> v != this)
        .anyMatch(parent -> parent.hasMethodInSelfOrParents(name, desc));
  }

  /**
   * @param name
   * 		Method name.
   * @param desc
   * 		Method descriptor.
   *
   * @return If the method exists in the current vertex or in any child vertex.
   */
  public boolean hasMethodInSelfOrChildren(@NotNull String name, @NotNull String desc) {
    if (hasMethod(name, desc))
      return true;
    return allChildren()
        .filter(v -> v != this)
        .anyMatch(parent -> parent.hasMethodInSelfOrChildren(name, desc));
  }

  /**
   * @return {@code true} if the class represented by this vertex is a library class.
   */
  public boolean isLibraryVertex() {
    return !isPrimary;
  }

  /**
   * @return {@code true} when the current vertex represents {@link Object}.
   */
  public boolean isJavaLangObject() {
    return getName().equals("java/lang/Object");
  }

  /**
   * @return {@code true} when a parent of this vertex, is this vertex.
   */
  public boolean isLoop() {
    String name = getName();
    return allParents()
        .anyMatch(v -> name.equals(v.getName()));
  }

  /**
   * @return {@code true} when the current vertex represents a {@code module-info}.
   */
  public boolean isModule() {
    return (getValue().access & Opcodes.ACC_MODULE) != 0 && getValue().superName == null;
  }

  /**
   * @param name
   * 		Method name.
   * @param desc
   * 		Method descriptor.
   *
   * @return {@code true} if method is an extension of an outside class's methods and thus should not be renamed.
   * {@code false} if the method is safe to rename.
   */
  public boolean isLibraryMethod(@NotNull String name, @NotNull String desc) {
    // Check against this definition
    if (!isPrimary && hasMethod(name, desc))
      return true;

    // Check parents.
    // If we extend a class with a library definition then it should be considered a library method.
    for (InheritanceVertex parent : getParents())
      if (parent.isLibraryMethod(name, desc))
        return true;

    // No library definition found, so its safe to rename.
    return false;
  }

  /**
   * @param vertex
   * 		Supposed child vertex.
   *
   * @return {@code true} if the vertex is of a child type to this vertex's {@link #getName() type}.
   */
  public boolean isParentOf(@NotNull InheritanceVertex vertex) {
    return vertex.getAllParents().contains(this);
  }

  /**
   * @param vertex
   * 		Supposed parent vertex.
   *
   * @return {@code true} if the vertex is of a parent type to this vertex's {@link #getName() type}.
   */
  public boolean isChildOf(@NotNull InheritanceVertex vertex) {
    return getAllParents().contains(vertex);
  }

  /**
   * @param vertex
   * 		Supposed vertex that belongs in the family.
   *
   * @return {@code true} if the vertex is a family member, but is not a child or parent of the current vertex.
   */
  public boolean isIndirectFamilyMember(@NotNull InheritanceVertex vertex) {
    return isIndirectFamilyMember(getFamily(true), vertex);
  }

  /**
   * @param family
   * 		Family to check in.
   * @param vertex
   * 		Supposed vertex that belongs in the family.
   *
   * @return {@code true} if the vertex is a family member, but is not a child or parent of the current vertex.
   */
  public boolean isIndirectFamilyMember(@NotNull Set<InheritanceVertex> family, @NotNull InheritanceVertex vertex) {
    return this != vertex &&
        family.contains(vertex) &&
        !isChildOf(vertex) &&
        !isParentOf(vertex);
  }

  /**
   * @param name
   * 		Name of parent type.
   *
   * @return {@code true} when this vertex has the given parent.
   */
  public boolean hasParent(@NotNull String name) {
    for (InheritanceVertex parent : getAllParents())
      if (name.equals(parent.getName()))
        return true;

    return false;
  }

  /**
   * @param name
   * 		Name of child type.
   *
   * @return {@code true} when this vertex has the given child.
   */
  public boolean hasChild(@NotNull String name) {
    for (InheritanceVertex child : getAllChildren())
      if (name.equals(child.getName()))
        return true;

    return false;
  }

  /**
   * @param includeObject
   *        {@code true} to include {@link Object} as a vertex.
   *
   * @return The entire class hierarchy.
   */
  @NotNull
  public Set<InheritanceVertex> getFamily(boolean includeObject) {
    Set<InheritanceVertex> vertices = new LinkedHashSet<>();
    visitFamily(vertices);
    if (!includeObject)
      vertices.removeIf(InheritanceVertex::isJavaLangObject);
    return vertices;
  }

  private void visitFamily(@NotNull Set<InheritanceVertex> vertices) {
    if (isModule())
      return;
    if (vertices.add(this) && !isJavaLangObject())
      for (InheritanceVertex vertex : getAllDirectVertices())
        vertex.visitFamily(vertices);
  }

  /**
   * @return All classes this extends or implements.
   */
  @NotNull
  public Set<InheritanceVertex> getAllParents() {
    return allParents().collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * @return All classes this extends or implements.
   */
  @NotNull
  public Stream<InheritanceVertex> allParents() {
    // Skip 1 to skip ourselves (which we use as the seed vertex)
    return Streams.recurseWithoutCycles(this, InheritanceVertex::getParents)
        .skip(1);
  }

  /**
   * @return Classes this directly extends or implements.
   */
  @NotNull
  public Set<InheritanceVertex> getParents() {
    Set<InheritanceVertex> parents = this.parents;
    if (parents == null) {
      synchronized (this) {
        if (isModule()) {
          parents = Collections.emptySet();
          this.parents = parents;
          return parents;
        }
        parents = this.parents;
        if (parents == null) {
          String name = getName();
          parents = new LinkedHashSet<>();
          String superName = value.superName;
          if (superName != null && !name.equals(superName)) {
            InheritanceVertex parentVertex = lookup.apply(superName);
            if (parentVertex != null)
              parents.add(parentVertex);
          }
          for (String itf : value.interfaces) {
            InheritanceVertex itfVertex = lookup.apply(itf);
            if (itfVertex != null && !name.equals(itf))
              parents.add(itfVertex);
          }
          this.parents = parents;
        }
      }
    }
    return parents;
  }

  /**
   * @return All classes extending or implementing this type.
   */
  @NotNull
  public Set<InheritanceVertex> getAllChildren() {
    return allChildren().collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * @return Stream of all classes extending or implementing this type.
   */
  @NotNull
  public Stream<InheritanceVertex> allChildren() {
    // Skip 1 to skip ourselves (which we use as the seed vertex)
    return Streams.recurseWithoutCycles(this, InheritanceVertex::getChildren)
        .skip(1);
  }

  /**
   * @return Classes that extend or implement this class.
   */
  @NotNull
  public Set<InheritanceVertex> getChildren() {
    Set<InheritanceVertex> children = this.children;
    if (children == null) {
      synchronized (this) {
        if (isModule()) {
          children = Collections.emptySet();
          this.children = children;
          return children;
        }
        children = this.children;
        if (children == null) {
          String name = getName();
          children = childrenLookup.apply(value.name)
              .stream()
              .filter(childName -> !name.equals(childName))
              .map(lookup)
              .filter(Objects::nonNull)
              .collect(Collectors.toCollection(LinkedHashSet::new));
          this.children = children;
        }
      }
    }
    return children;
  }

  /**
   * @return All direct parents and child vertices.
   */
  @NotNull
  public Set<InheritanceVertex> getAllDirectVertices() {
    Set<InheritanceVertex> set = new HashSet<>(getParents());
    set.addAll(getChildren());
    return set;
  }

  /**
   * Clears cached {@link #getParents()} and {@link #getChildren()} values.
   */
  public void clearCachedVertices() {
    synchronized (this) {
      parents = null;
      children = null;
    }
  }


  /**
   * @return {@link #getValue() wrapped class's} name
   */
  @NotNull
  public String getName() {
    return value.name;
  }

  /**
   * @return Wrapped class info.
   */
  @NotNull
  public ClassNode getValue() {
    return value;
  }

  /**
   * @param value
   * 		New wrapped class info.
   */
  public void setValue(@NotNull ClassNode value) {
    this.value = value;

    // Reset children & parent sets
    synchronized (this) {
      children = null;
      parents = null;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InheritanceVertex vertex = (InheritanceVertex) o;
    return Objects.equals(getName(), vertex.getName());
  }

  @Override
  public int hashCode() {
    return getName().hashCode();
  }

  @Override
  public String toString() {
    return getName();
  }
}
