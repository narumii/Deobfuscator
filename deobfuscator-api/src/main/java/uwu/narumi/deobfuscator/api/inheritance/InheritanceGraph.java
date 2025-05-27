package uwu.narumi.deobfuscator.api.inheritance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import uwu.narumi.deobfuscator.api.classpath.ClassProvider;
import uwu.narumi.deobfuscator.api.classpath.CombinedClassProvider;
import uwu.narumi.deobfuscator.api.classpath.JvmClassProvider;
import uwu.narumi.deobfuscator.api.context.Context;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class inheritance graph utility.
 *
 * @author Matt Coley
 */
// Copied from https://github.com/Col-E/Recaf/blob/ac6e07cbaf168a1f2093e71a39215bda8a00402d/recaf-core/src/main/java/software/coley/recaf/services/inheritance/InheritanceGraph.java
public class InheritanceGraph {
  private static final Logger LOGGER = LogManager.getLogger();

  /** Vertex used for classes that are not found in the workspace. */
  private static final InheritanceVertex STUB = new InheritanceStubVertex();
  private static final String OBJECT = "java/lang/Object";
  private final Map<String, Set<String>> parentToChild = new ConcurrentHashMap<>();
  private final Map<String, InheritanceVertex> vertices = new ConcurrentHashMap<>();
  private final Set<String> stubs = ConcurrentHashMap.newKeySet();
  private final Function<String, InheritanceVertex> vertexProvider = createVertexProvider();
  private final ClassProvider classProvider;
  private final ClassProvider librariesClassProvider;

  /**
   * Create an inheritance graph.
   */
  public InheritanceGraph(@NotNull Context context) {
    this.librariesClassProvider = new CombinedClassProvider(context.getLibraries(), JvmClassProvider.INSTANCE);
    this.classProvider = new CombinedClassProvider(context, this.librariesClassProvider);

    // Populate downwards (parent --> child) lookup
    refreshChildLookup();
  }

  /**
   * Refresh parent-to-child lookup.
   */
  private void refreshChildLookup() {
    // Clear
    parentToChild.clear();

    // Repopulate
    classProvider.getLoadedClasses().stream()
        .map(classProvider::getClassInfo)
        .filter(Objects::nonNull)
        .forEach(this::populateParentToChildLookup);
  }

  /**
   * Populate a references from the given child class to the parent class.
   *
   * @param name
   * 		Child class name.
   * @param parentName
   * 		Parent class name.
   */
  private void populateParentToChildLookup(@NotNull String name, @NotNull String parentName) {
    parentToChild.computeIfAbsent(parentName, k -> ConcurrentHashMap.newKeySet()).add(name);
  }

  /**
   * Populate all references from the given child class to its parents.
   *
   * @param info
   * 		Child class.
   */
  private void populateParentToChildLookup(@NotNull ClassNode info) {
    populateParentToChildLookup(info, Collections.newSetFromMap(new IdentityHashMap<>()));
  }

  /**
   * Populate all references from the given child class to its parents.
   *
   * @param info
   * 		Child class.
   * @param visited
   * 		Classes already visited in population.
   */
  private void populateParentToChildLookup(@NotNull ClassNode info, @NotNull Set<ClassNode> visited) {
    // Skip if already visited
    if (!visited.add(info))
      return;

    // Skip module classes
    if ((info.access & Opcodes.ACC_MODULE) != 0)
      return;

    // Add direct parent
    String name = info.name;
    String superName = info.superName;
    if (superName != null)
      populateParentToChildLookup(name, superName);

    // Visit parent
    InheritanceVertex superVertex = vertexProvider.apply(superName);
    if (superVertex != null && !superVertex.isJavaLangObject() && !superVertex.isLoop())
      populateParentToChildLookup(superVertex.getValue(), visited);

    // Add direct interfaces
    for (String itf : info.interfaces) {
      populateParentToChildLookup(name, itf);

      // Visit interfaces
      InheritanceVertex interfaceVertex = vertexProvider.apply(itf);
      if (interfaceVertex != null)
        populateParentToChildLookup(interfaceVertex.getValue(), visited);
    }
  }

  /**
   * Remove all references from the given child class to its parents.
   *
   * @param info
   * 		Child class.
   */
  private void removeParentToChildLookup(@NotNull ClassNode info) {
    String superName = info.superName;
    if (superName != null)
      removeParentToChildLookup(info.name, superName);
    for (String itf : info.interfaces)
      removeParentToChildLookup(info.name, itf);
  }

  /**
   * Remove a references from the given child class to the parent class.
   *
   * @param name
   * 		Child class name.
   * @param parentName
   * 		Parent class name.
   */
  private void removeParentToChildLookup(@NotNull String name, @NotNull String parentName) {
    Set<String> children = parentToChild.get(parentName);
    if (children != null)
      children.remove(name);
    InheritanceVertex parentVertex = getVertex(parentName);
    InheritanceVertex childVertex = getVertex(name);
    if (parentVertex != null) parentVertex.clearCachedVertices();
    if (childVertex != null) childVertex.clearCachedVertices();
  }

  /**
   * Removes the given class from the graph.
   *
   * @param cls
   * 		Class that was removed.
   */
  private void removeClass(@NotNull ClassNode cls) {
    removeParentToChildLookup(cls);

    String name = cls.name;
    vertices.remove(name);
  }


  /**
   * @param parent
   * 		Parent to find children of.
   *
   * @return Direct extensions/implementations of the given parent.
   */
  @NotNull
  private Set<String> getDirectChildren(@NotNull String parent) {
    return parentToChild.getOrDefault(parent, Collections.emptySet());
  }

  /**
   * @param name
   * 		Class name.
   *
   * @return Vertex in graph of class. {@code null} if no such class was found in the inputs.
   */
  @Nullable
  public InheritanceVertex getVertex(@NotNull String name) {
    InheritanceVertex vertex = vertices.get(name);
    if (vertex == null && !stubs.contains(name)) {
      // Vertex does not exist and was not marked as a stub.
      // We want to look up the vertex for the given class and figure out if its valid or needs to be stubbed.
      InheritanceVertex provided = vertexProvider.apply(name);
      if (provided == STUB || provided == null) {
        // Provider yielded either a stub OR no result. Discard it.
        stubs.add(name);
      } else {
        // Provider yielded a valid vertex. Update the return value and record it in the map.
        vertices.put(name, provided);
        vertex = provided;
      }
    }
    return vertex;
  }

  /**
   * @param name
   * 		Class name.
   * @param includeObject
   *        {@code true} to include {@link Object} as a vertex.
   *
   * @return Complete inheritance family of the class.
   */
  @NotNull
  public Set<InheritanceVertex> getVertexFamily(@NotNull String name, boolean includeObject) {
    InheritanceVertex vertex = getVertex(name);
    if (vertex == null)
      return Collections.emptySet();
    if (vertex.isModule())
      return Collections.singleton(vertex);
    return vertex.getFamily(includeObject);
  }

  /**
   * @param first
   * 		First class name.
   * @param second
   * 		Second class name.
   *
   * @return Common parent of the classes.
   */
  @NotNull
  public String getCommon(@NotNull String first, @NotNull String second) {
    // Full upwards hierarchy for the first
    InheritanceVertex vertex = getVertex(first);
    if (vertex == null) {
      printCantFindClass(first);
      return OBJECT;
    }
    if (OBJECT.equals(first) || OBJECT.equals(second)) {
      return OBJECT;
    }

    Set<String> firstParents = vertex.allParents()
        .map(InheritanceVertex::getName)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    firstParents.add(first);

    // Ensure 'Object' is last
    firstParents.remove(OBJECT);
    firstParents.add(OBJECT);

    // Base case
    if (firstParents.contains(second))
      return second;

    // Iterate over second's parents via breadth-first-search
    Queue<String> queue = new LinkedList<>();
    queue.add(second);
    do {
      // Item to fetch parents of
      String next = queue.poll();
      if (next == null || next.equals(OBJECT))
        break;

      InheritanceVertex nextVertex = getVertex(next);
      if (nextVertex == null) {
        printCantFindClass(next);
        break;
      }

      for (String parent : nextVertex.getParents().stream()
          .map(InheritanceVertex::getName).toList()) {
        if (!parent.equals(OBJECT)) {
          // Parent in the set of visited classes? Then its valid.
          if (firstParents.contains(parent))
            return parent;
          // Queue up the parent
          queue.add(parent);
        }
      }
    } while (!queue.isEmpty());

    // Fallback option
    return OBJECT;
  }

  private void printCantFindClass(String className) {
    LOGGER.warn("Can't find class '{}'. Computed frames might be wrong. " +
        "If you want a runnable deobfuscated jar then add a missing lib using 'DeobfuscatorOptions#libraries'.", className);
  }

  @NotNull
  private Function<String, InheritanceVertex> createVertexProvider() {
    return name -> {
      // Edge case handling for 'java/lang/Object' doing a parent lookup.
      // There is no parent, do not use STUB.
      if (name == null)
        return null;

      // Edge case handling for arrays. There is no object typing of arrays.
      if (name.isEmpty() || name.charAt(0) == '[')
        return null;

      // Find class in workspace, if not found yield stub.
      ClassNode result = this.classProvider.getClassInfo(name);
      if (result == null) {
        return STUB;
      }

      // Map class to vertex.
      //ResourcePathNode resourcePath = result.getPathOfType(WorkspaceResource.class);
      //boolean isPrimary = resourcePath != null && resourcePath.isPrimary();
      //ClassInfo info = result.getValue();
      return new InheritanceVertex(result, this::getVertex, this::getDirectChildren, this.librariesClassProvider.getClass(name) == null);
    };
  }

  private static class InheritanceStubVertex extends InheritanceVertex {
    private InheritanceStubVertex() {
      super(new ClassNode(), in -> null, in -> null, false);
    }

    @Override
    public boolean hasField(@NotNull String name, @NotNull String desc) {
      return false;
    }

    @Override
    public boolean hasMethod(@NotNull String name, @NotNull String desc) {
      return false;
    }

    @Override
    public boolean isJavaLangObject() {
      return false;
    }

    @Override
    public boolean isParentOf(@NotNull InheritanceVertex vertex) {
      return false;
    }

    @Override
    public boolean isChildOf(@NotNull InheritanceVertex vertex) {
      return false;
    }

    @Override
    public boolean isIndirectFamilyMember(@NotNull InheritanceVertex vertex) {
      return false;
    }

    @Override
    public boolean isIndirectFamilyMember(@NotNull Set<InheritanceVertex> family, @NotNull InheritanceVertex vertex) {
      return false;
    }

    @NotNull
    @Override
    public Set<InheritanceVertex> getFamily(boolean includeObject) {
      return Collections.emptySet();
    }

    @NotNull
    @Override
    public Set<InheritanceVertex> getAllParents() {
      return Collections.emptySet();
    }

    @NotNull
    @Override
    public Stream<InheritanceVertex> allParents() {
      return Stream.empty();
    }

    @NotNull
    @Override
    public Set<InheritanceVertex> getParents() {
      return Collections.emptySet();
    }

    @NotNull
    @Override
    public Set<InheritanceVertex> getAllChildren() {
      return Collections.emptySet();
    }

    @NotNull
    @Override
    public Set<InheritanceVertex> getChildren() {
      return Collections.emptySet();
    }

    @NotNull
    @Override
    public Set<InheritanceVertex> getAllDirectVertices() {
      return Collections.emptySet();
    }

    @NotNull
    @Override
    public String getName() {
      return "$$STUB$$";
    }
  }
}
