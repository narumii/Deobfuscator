package dev.xdark.ssvm.operation;

import dev.xdark.ssvm.LanguageSpecification;
import dev.xdark.ssvm.RuntimeResolver;
import dev.xdark.ssvm.classloading.BootClassFinder;
import dev.xdark.ssvm.classloading.ClassDefiner;
import dev.xdark.ssvm.classloading.ClassDefinitionOption;
import dev.xdark.ssvm.classloading.ClassLoaderData;
import dev.xdark.ssvm.classloading.ClassLoaders;
import dev.xdark.ssvm.classloading.ClassStorage;
import dev.xdark.ssvm.classloading.ParsedClassData;
import dev.xdark.ssvm.execution.Locals;
import dev.xdark.ssvm.execution.PanicException;
import dev.xdark.ssvm.execution.VMException;
import dev.xdark.ssvm.inject.InjectedClassLayout;
import dev.xdark.ssvm.jvmti.VMEventCollection;
import dev.xdark.ssvm.memory.allocation.MemoryData;
import dev.xdark.ssvm.memory.management.MemoryManager;
import dev.xdark.ssvm.mirror.MirrorFactory;
import dev.xdark.ssvm.mirror.member.JavaField;
import dev.xdark.ssvm.mirror.member.JavaMethod;
import dev.xdark.ssvm.mirror.member.MemberIdentifier;
import dev.xdark.ssvm.mirror.member.area.ClassArea;
import dev.xdark.ssvm.mirror.member.area.EmptyClassArea;
import dev.xdark.ssvm.mirror.member.area.SimpleClassArea;
import dev.xdark.ssvm.mirror.type.ClassLinkage;
import dev.xdark.ssvm.mirror.type.InitializationState;
import dev.xdark.ssvm.mirror.type.InstanceClass;
import dev.xdark.ssvm.mirror.type.JavaClass;
import dev.xdark.ssvm.symbol.Primitives;
import dev.xdark.ssvm.symbol.Symbols;
import dev.xdark.ssvm.thread.ThreadManager;
import dev.xdark.ssvm.util.AsmUtil;
import dev.xdark.ssvm.util.Assertions;
import dev.xdark.ssvm.util.CloseableLock;
import dev.xdark.ssvm.value.InstanceValue;
import dev.xdark.ssvm.value.ObjectValue;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Default implementation.
 *
 * @author xDark
 */
public final class DefaultClassOperations implements ClassOperations {

  private final MirrorFactory mirrorFactory;
  private final MemoryManager memoryManager;
  private final ThreadManager threadManager;
  private final BootClassFinder bootClassFinder;
  private final RuntimeResolver runtimeResolver;
  private final Symbols symbols;
  private final Primitives primitives;
  private final ClassLoaders classLoaders;
  private final ClassDefiner classDefiner;
  private final ClassStorage classStorage;
  private final VMEventCollection eventCollection;
  private final VMOperations ops;

  public DefaultClassOperations(MirrorFactory mirrorFactory, MemoryManager memoryManager, ThreadManager threadManager, BootClassFinder bootClassFinder, RuntimeResolver runtimeResolver, Symbols symbols, Primitives primitives, ClassLoaders classLoaders, ClassDefiner classDefiner, ClassStorage classStorage, VMEventCollection eventCollection, VMOperations ops) {
    this.mirrorFactory = mirrorFactory;
    this.memoryManager = memoryManager;
    this.threadManager = threadManager;
    this.bootClassFinder = bootClassFinder;
    this.runtimeResolver = runtimeResolver;
    this.symbols = symbols;
    this.primitives = primitives;
    this.classLoaders = classLoaders;
    this.classDefiner = classDefiner;
    this.classStorage = classStorage;
    this.eventCollection = eventCollection;
    this.ops = ops;
  }

  @Override
  public void link(@NotNull InstanceClass instanceClass) {
    InitializationState state = instanceClass.state();
    state.lock();
    state.set(InstanceClass.State.IN_PROGRESS);
    try {
      eventCollection.getClassPrepare().invoke(instanceClass);
      ClassLinkage linkage = instanceClass.linkage();
      ClassNode node = instanceClass.getNode();
      String superName = node.superName;
      List<String> interfaces = node.interfaces;
      if (superName != null) {
        linkage.setSuperClass((InstanceClass) findClass(instanceClass, superName, false));
      }
      // Create method and field area
      MirrorFactory mf = this.mirrorFactory;
      // Set methods
      List<MethodNode> methods = node.methods;
      List<JavaMethod> allMethods = new ArrayList<>(methods.size());
      for (int i = 0, j = methods.size(); i < j; i++) {
        allMethods.add(mf.newMethod(instanceClass, methods.get(i), i));
      }
      linkage.setMethodArea(new SimpleClassArea<>(allMethods));
      List<JavaField> virtualFields = new ArrayList<>();
      InstanceClass jc = instanceClass.getSuperClass();
      JavaField lastField = null;
      while (jc != null) {
        ClassArea<JavaField> area = jc.virtualFieldArea();
        // May be java/lang/Class calling to java/lang/Object
        if (area == null) {
          Assertions.check(jc == symbols.java_lang_Object(), "null area is only allowed for java/lang/Object");
        } else {
          JavaField field = area.stream()
              .filter(x -> (x.getModifiers() & Opcodes.ACC_STATIC) == 0)
              .max(Comparator.comparingLong(JavaField::getOffset))
              .orElse(null);
          if (field != null && (lastField == null || field.getOffset() > lastField.getOffset())) {
            lastField = field;
          }
        }
        jc = jc.getSuperClass();
      }
      long offset;
      MemoryManager memoryManager = this.memoryManager;
      if (lastField != null) {
        offset = lastField.getOffset();
        offset += safeSizeOf(lastField.getDesc());
      } else {
        offset = memoryManager.valueBaseOffset(instanceClass);
      }

      List<FieldNode> fields = node.fields;
      int slot = 0;
      for (int i = 0, j = fields.size(); i < j; i++) {
        FieldNode fieldNode = fields.get(i);
        if ((fieldNode.access & Opcodes.ACC_STATIC) == 0) {
          JavaField field = mf.newField(instanceClass, fieldNode, slot++, offset);
          offset += safeSizeOf(field.getDesc());
          virtualFields.add(field);
        }
      }
      linkage.setVirtualFieldArea(new SimpleClassArea<>(virtualFields));
      linkage.setOccupiedInstanceSpace(offset - memoryManager.valueBaseOffset(instanceClass));
      int slotOffset = slot;
      // Static fields are stored right after java/lang/Class virtual fields
      // At this point of linkage java/lang/Class must already set its virtual
      // fields as we are doing it before (see above)
      InstanceClass jlc = symbols.java_lang_Class();
      if (jlc == null) {
        // Linking it now?
        Assertions.check("java/lang/Class".equals(node.name), "bad first class for linking");
        jlc = instanceClass;
      }
      Assertions.notNull(jlc, "null java/lang/Class");
      ClassArea<JavaField> jlcFieldArea = jlc.virtualFieldArea();
      if (jlcFieldArea == null) {
        Assertions.check("java/lang/Object".equals(node.name), "virtual field area");
        // No static fields allowed here.
        linkage.setStaticFieldArea(EmptyClassArea.create());
        linkage.setOccupiedStaticSpace(0L);
      } else {
        JavaField maxVirtualField = jlcFieldArea.stream()
            .max(Comparator.comparingLong(JavaField::getOffset))
            .orElseThrow(() -> new PanicException("No fields in java/lang/Class"));
        offset = maxVirtualField.getOffset() + memoryManager.sizeOfType(maxVirtualField.getType()); // TODO calling getType may lead to exception
        long baseStaticOffset = offset;
        List<JavaField> staticFields = new ArrayList<>(fields.size() - slot);
        for (int i = 0, j = fields.size(); i < j; i++) {
          FieldNode fieldNode = fields.get(i);
          if ((fieldNode.access & Opcodes.ACC_STATIC) != 0) {
            JavaField field = mf.newField(instanceClass, fieldNode, slot++, offset);
            offset += safeSizeOf(field.getDesc());
            staticFields.add(field);
          }
        }
        linkage.setStaticFieldArea(new SimpleClassArea<>(staticFields, slotOffset));
        linkage.setOccupiedStaticSpace(offset - baseStaticOffset);
      }
      // Load interfaces now
      if (!interfaces.isEmpty()) {
        InstanceClass[] classes = new InstanceClass[interfaces.size()];
        for (int i1 = 0; i1 < interfaces.size(); i1++) {
          classes[i1] = (InstanceClass) findClass(instanceClass, interfaces.get(i1), false);
        }
        linkage.setInterfaces(Arrays.asList(classes));
      } else {
        linkage.setInterfaces(Collections.emptyList());
      }
      if (jlc.getOop() != null) {
        // VM might be still starting up
        // All classes without mirrors will be fixed later
        instanceClass.setOop(memoryManager.newClassOop(instanceClass));
      }
      eventCollection.getClassLink().invoke(instanceClass);
      // After we're done, set the state back to PENDING,
      // so that the class can be initialized
      state.set(InstanceClass.State.PENDING);
    } catch (VMException ex) {
      state.set(InstanceClass.State.FAILED);
      throwClassException(ex);
    } finally {
      state.condition().signalAll();
      state.unlock();
    }
  }

  @Override
  public void initialize(@NotNull InstanceClass instanceClass) {
    InitializationState state = instanceClass.state();
    state.lock();
    if (state.is(InstanceClass.State.COMPLETE) || state.is(InstanceClass.State.IN_PROGRESS)) {
      state.unlock();
      return;
    }
    if (state.is(InstanceClass.State.FAILED)) {
      state.unlock();
      ops.throwException(symbols.java_lang_NoClassDefFoundError(), instanceClass.getInternalName());
    }
    state.set(InstanceClass.State.IN_PROGRESS);
    try {
      // Initialize hierarchy
      InstanceClass superClass = instanceClass.getSuperClass();
      if (superClass != null) {
        initialize(superClass);
      }
      // note: interfaces are *not* initialized here
      initializeStaticFields(instanceClass);
      JavaMethod clinit = instanceClass.getMethod("<clinit>", "()V");
      if (clinit != null) {
        Locals locals = threadManager.currentThreadStorage().newLocals(clinit);
        ops.invokeVoid(clinit, locals);
      }
    } catch (VMException ex) {
      state.set(InstanceClass.State.FAILED);
      throwClassException(ex);
    } finally {
      state.condition().signalAll();
      state.unlock();
    }
  }

  @Override
  public boolean isInstanceOf(@NotNull ObjectValue value, @NotNull JavaClass type) {
    if (value.isNull()) {
      return false;
    }
    return type.isAssignableFrom(value.getJavaClass());
  }

  @Override
  public @NotNull JavaClass findClass(JavaClass klass, String internalName, boolean initialize) {
    return findClass0(classLoaders.getClassLoaderData(klass), klass.getClassLoader(), internalName, initialize, true);
  }

  @Override
  public @NotNull JavaClass findClass(ObjectValue classLoader, String internalName, boolean initialize) {
    return findClass0(classLoaders.getClassLoaderData(classLoader), classLoader, internalName, initialize, true);
  }

  @Override
  public JavaClass findBootstrapClassOrNull(String internalName, boolean initialize) {
    ObjectValue cl = memoryManager.nullValue();
    return findClass0(classLoaders.getClassLoaderData(cl), cl, internalName, initialize, false);
  }

  @Override
  public @NotNull InstanceClass defineClass(ObjectValue classLoader, ParsedClassData data, ObjectValue protectionDomain, String source, int options) {
    ClassReader reader = data.getClassReader();
    InstanceClass jc = mirrorFactory.newInstanceClass(classLoader, reader, data.getNode());
    InitializationState state = jc.state();
    state.lock();
    try {
      if ((options & ClassDefinitionOption.ANONYMOUS) == 0) {
        ClassLoaderData classLoaderData = classLoaders.getClassLoaderData(classLoader);
        if (!classLoaderData.linkClass(jc)) {
          ops.throwException(symbols.java_lang_NoClassDefFoundError(), "Duplicate class: " + reader.getClassName());
        }
      }
      link(jc);
      if ((options & ClassDefinitionOption.ANONYMOUS) != 0) {
        if (!classLoaders.createAnonymousClassLoaderData(jc).linkClass(jc)) {
          ops.throwException(symbols.java_lang_NoClassDefFoundError(), "Failed to link to anonymous data: " + reader.getClassName());
        }
      }
      if (!classLoader.isNull()) {
        ops.putReference(jc.getOop(), "classLoader", "Ljava/lang/ClassLoader;", classLoader);
        // Narumii start - Fix loading classes in root directory with no package
        ObjectValue unnamedModule = ops.getReference(classLoader, "unnamedModule", "Ljava/lang/Module;");
        ops.putReference(jc.getOop(), "module", "Ljava/lang/Module;", unnamedModule);
        // Narumii end
      }
      if (!protectionDomain.isNull()) {
        ops.putReference(jc.getOop(), InjectedClassLayout.java_lang_Class_protectionDomain.name(), InjectedClassLayout.java_lang_Class_protectionDomain.descriptor(), protectionDomain);
      }
      classStorage.register(jc);
    } finally {
      state.unlock();
    }
    return jc;
  }

  @Override
  public @NotNull InstanceClass defineClass(ObjectValue classLoader, String name, byte[] b, int off, int len, ObjectValue protectionDomain, String source, int options) {
    VMOperations ops = this.ops;
    if ((off | len | (off + len) | (b.length - (off + len))) < 0) {
      ops.throwException(symbols.java_lang_ArrayIndexOutOfBoundsException());
    }
    ParsedClassData data = classDefiner.parseClass(name, b, off, len, source);
    if (data == null) {
      ops.throwException(symbols.java_lang_NoClassDefFoundError(), name);
    }
    String classReaderName = data.getClassReader().getClassName();
    if (name == null) {
      name = classReaderName;
    } else if (!classReaderName.equals(name.replace('.', '/'))) {
      ops.throwException(symbols.java_lang_ClassNotFoundException(), "Expected class name " + classReaderName.replace('/', '.') + " but received: " + name);
    }
    if (name.contains("[") || name.contains("(") || name.contains(")") || name.contains(";")) {
      ops.throwException(symbols.java_lang_NoClassDefFoundError(), "Bad class name: " + classReaderName);
    }
    return defineClass(classLoader, data, protectionDomain, source, options);
  }

  @Override
  public @NotNull JavaClass findClass(JavaClass klass, Type type, boolean initialize) {
    return findClass(klass.getClassLoader(), type, initialize);
  }

  @Override
  public @NotNull JavaClass findClass(ObjectValue classLoader, Type type, boolean initialize) {
    int sort = type.getSort();
    if (sort == Type.ARRAY) {
      Type primitive = type.getElementType();
      int psort = primitive.getSort();
      if (psort < Type.ARRAY) {
        JavaClass cls = lookupPrimitive(psort);
        for (int i = 0, j = type.getDimensions(); i < j;i++) {
          cls = cls.newArrayClass();
        }
        return cls;
      }
    }
    if (sort < Type.ARRAY) {
      return lookupPrimitive(sort);
    }
    return findClass(classLoader, type.getInternalName(), initialize);
  }

  private JavaClass lookupPrimitive(int sort) {
    Primitives primitives = this.primitives;
    switch (sort) {
      case Type.VOID:
        return primitives.voidPrimitive();
      case Type.BOOLEAN:
        return primitives.booleanPrimitive();
      case Type.CHAR:
        return primitives.charPrimitive();
      case Type.BYTE:
        return primitives.bytePrimitive();
      case Type.SHORT:
        return primitives.shortPrimitive();
      case Type.INT:
        return primitives.intPrimitive();
      case Type.FLOAT:
        return primitives.floatPrimitive();
      case Type.LONG:
        return primitives.longPrimitive();
      case Type.DOUBLE:
        return primitives.doublePrimitive();
    }
    throw new PanicException("unreachable code");
  }

  private JavaClass lookupPrimitiveOrNull(char desc) {
    Primitives primitives = this.primitives;
    switch (desc) {
      case 'Z':
        return primitives.booleanPrimitive();
      case 'C':
        return primitives.charPrimitive();
      case 'B':
        return primitives.bytePrimitive();
      case 'S':
        return primitives.shortPrimitive();
      case 'I':
        return primitives.intPrimitive();
      case 'F':
        return primitives.floatPrimitive();
      case 'J':
        return primitives.longPrimitive();
      case 'D':
        return primitives.doublePrimitive();
    }
    return null;
  }

  private void initializeStaticFields(InstanceClass instanceClass) {
    InstanceValue oop = instanceClass.getOop();
    Assertions.notNull(oop, "oop not created");
    MemoryManager memoryManager = this.memoryManager;
    MemoryData data = oop.getData();
    for (JavaField field : instanceClass.staticFieldArea().list()) {
      MemberIdentifier identifier = field.getIdentifier();
      String desc = identifier.getDesc();
      FieldNode fn = field.getNode();
      Object cst = fn.value;
      if (cst == null) {
        cst = AsmUtil.getDefaultValue(desc);
      }
      long offset = field.getOffset();
      switch (desc.charAt(0)) {
        case 'J':
          data.writeLong(offset, (Long) cst);
          break;
        case 'D':
          data.writeLong(offset, Double.doubleToRawLongBits((Double) cst));
          break;
        case 'I':
          data.writeInt(offset, (Integer) cst);
          break;
        case 'F':
          data.writeInt(offset, Float.floatToRawIntBits((Float) cst));
          break;
        case 'C':
          data.writeChar(offset, (char) ((Integer) cst).intValue());
          break;
        case 'S':
          data.writeShort(offset, ((Integer) cst).shortValue());
          break;
        case 'B':
        case 'Z':
          data.writeByte(offset, ((Integer) cst).byteValue());
          break;
        default:
          memoryManager.writeValue(oop, offset, cst == null ? memoryManager.nullValue() : ops.referenceValue(cst));
      }
    }
  }

  private long safeSizeOf(String desc) {
    Type type = Type.getType(desc);
    int sort = type.getSort();
    if (sort < Type.ARRAY) {
      return LanguageSpecification.primitiveSize(sort);
    }
    // Anything else is a reference.
    return memoryManager.objectSize();
  }

  private void throwClassException(VMException ex) {
    InstanceValue oop = ex.getOop();
    Symbols symbols = this.symbols;
    if (!symbols.java_lang_Error().isAssignableFrom(oop.getJavaClass())) {
      InstanceClass jc = symbols.java_lang_ExceptionInInitializerError();
      initialize(jc);
      InstanceValue cause = oop;
      oop = memoryManager.newInstance(jc);
      // Can't use newException here
      JavaMethod init = jc.getMethod("<init>", "(Ljava/lang/Throwable;)V");
      Locals locals = threadManager.currentThreadStorage().newLocals(init);
      locals.setReference(0, oop);
      locals.setReference(1, cause);
      ops.invokeVoid(init, locals);
      throw new VMException(oop);
    }
    throw ex;
  }

  private JavaClass findClass0(ClassLoaderData data, ObjectValue classLoader, String internalName, boolean initialize, boolean _throw) {
    int dimensions = 0;
    while (internalName.charAt(dimensions) == '[') {
      dimensions++;
    }
    VMOperations ops = this.ops;
    if (dimensions >= LanguageSpecification.ARRAY_DIMENSION_LIMIT) {
      ops.throwException(symbols.java_lang_ClassNotFoundException(), internalName);
    }
    JavaClass klass;
    if(dimensions > 0) {
      if (internalName.charAt(dimensions) != 'L') {
        // Primitive array
        klass = lookupPrimitiveOrNull(internalName.charAt(dimensions));
        if (klass == null) {
          ops.throwException(symbols.java_lang_ClassNotFoundException(), internalName);
          return null;
        }

        while (dimensions-- != 0) {
          klass = klass.newArrayClass();
        }
        return klass;
      }
    }
    String trueName = dimensions == 0 ? internalName : internalName.substring(dimensions + 1, internalName.length() - 1);
    try (CloseableLock lock = data.lock()) {
      klass = data.getClass(trueName);
      if (klass == null) {
        if (classLoader.isNull()) {
          ParsedClassData cdata = bootClassFinder.findBootClass(trueName);
          if (cdata != null) {
            klass = defineClass(classLoader, cdata, memoryManager.nullValue(), "JVM_DefineClass");
          }
        } else {
          // Ask Java world
          JavaMethod method = runtimeResolver.resolveVirtualMethod(classLoader, "loadClass", "(Ljava/lang/String;Z)Ljava/lang/Class;");
          Locals locals = threadManager.currentThreadStorage().newLocals(method);
          locals.setReference(0, classLoader);
          locals.setReference(1, ops.newUtf8(trueName.replace('/', '.')));
          locals.setInt(2, initialize ? 1 : 0);
          InstanceValue result = ops.checkNotNull(ops.invokeReference(method, locals));
          klass = classStorage.lookup(result);
        }
        if (klass == null) {
          if (_throw) {
            ops.throwException(symbols.java_lang_ClassNotFoundException(), internalName.replace('/', '.'));
          }
          dimensions = 0;
          initialize = false;
        }
      }
    }
    if (initialize) {
      if (klass instanceof InstanceClass) {
        initialize((InstanceClass) klass);
      }
    }
    while (dimensions-- != 0) {
      klass = klass.newArrayClass();
    }
    return klass;
  }
}