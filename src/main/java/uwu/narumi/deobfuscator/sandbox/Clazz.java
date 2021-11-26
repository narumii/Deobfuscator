package uwu.narumi.deobfuscator.sandbox;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/*
    Fucking hardcoded shit
 */
public class Clazz {

    protected static final Logger LOGGER = LogManager.getLogger(Clazz.class);

    private final Class<?> clazz;

    public Clazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Object invoke(String methodName, String methodDesc, Object reference, Object... arguments) {
        return invoke(methodName, MethodType.fromMethodDescriptorString(methodDesc, Clazz.class.getClassLoader()), reference, arguments);
    }

    public Object invoke(String methodName, Class<?> returnType, Class<?>[] parameters, Object reference, Object... arguments) {
        return invoke(methodName, MethodType.methodType(returnType, parameters), reference, arguments);
    }

    /*
    Should we use Lookup for this?
     */
    public Object invoke(String methodName, MethodType methodType, Object reference, Object... arguments) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, methodType.parameterArray());
            if (!method.isAccessible())
                method.trySetAccessible();

            return method.invoke(reference, arguments);
        } catch (NoSuchMethodError | NoSuchMethodException ignored) {
            LOGGER.debug("Method {}{} not found in class {}", methodName, methodType.toMethodDescriptorString(), clazz.getName());
        } catch (Throwable e) {
            if (e.getCause() instanceof NoSuchMethodError | e.getCause() instanceof NoSuchMethodException) {
                LOGGER.debug("Method {}{} not found in class {}", methodName, methodType.toMethodDescriptorString(), clazz.getName());
            } else {
                e.printStackTrace();
                LOGGER.error("Can't invoke method [name: {}, desc: {}]", methodName, methodType.toMethodDescriptorString());
                LOGGER.debug("ERROR", e);
            }
        }

        return null;
    }

    /*
        Lookup maybe? xd
     */
    public Object get(String fieldName, Object reference) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            if (!field.isAccessible())
                field.trySetAccessible();

            return field.get(reference);
        } catch (Exception e) {
            LOGGER.error("Can't get field value [name: {}]", fieldName);
            LOGGER.debug("ERROR", e);
        }

        return null;
    }

    public Class<?> getClazz() {
        return clazz;
    }
}
