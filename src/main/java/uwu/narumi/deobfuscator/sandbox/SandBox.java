package uwu.narumi.deobfuscator.sandbox;

import org.apache.logging.log4j.core.util.Loader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import uwu.narumi.deobfuscator.helper.ClassHelper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
   Fucking hardcoded shit xd
 */
public class SandBox extends ClassLoader {

    private static final Map<String, Clazz> classes = new ConcurrentHashMap<>();
    private static SandBox INSTANCE;

    private SandBox() {
        super("SandBoxClassLoader", Loader.getClassLoader());

        if (INSTANCE != null)
            throw new IllegalArgumentException();
    }

    public synchronized static SandBox getInstance() {
        if (INSTANCE == null)
            INSTANCE = new SandBox();

        return INSTANCE;
    }

    public SandBox put(ClassNode... classNodes) {
        buildClasses(classNodes);
        return this;
    }

    public Clazz get(ClassNode classNode) {
        return get(classNode.name);
    }

    public Clazz get(String name) {
        return classes.get(name.replace('/', '.'));
    }

    private void buildClasses(ClassNode... classNodes) {
        for (ClassNode classNode : classNodes) {
            buildClass(classNode);
        }
    }

    private Clazz buildClass(ClassNode classNode) {
        if (classes.containsKey(classNode.name.replace('/', '.')))
            return classes.get(classNode.name.replace('/', '.'));

        byte[] bytes = ClassHelper.classToBytes(ClassHelper.copy(classNode), ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        Clazz clazz = new Clazz(defineClass(classNode.name.replace('/', '.'), bytes, 0, bytes.length, this.getClass().getProtectionDomain()));
        resolveClass(clazz.getClazz());

        classes.put(classNode.name.replace('/', '.'), clazz);
        return clazz;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        name = name.replace('/', '.');
        if (classes.containsKey(name))
            return classes.get(name).getClazz();

        return super.findClass(name);
    }
}
