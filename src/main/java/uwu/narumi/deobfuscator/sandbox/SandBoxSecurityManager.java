package uwu.narumi.deobfuscator.sandbox;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.Permission;

/*
    Shit SecurityManager that protect your computer from SandBox exploits
 */
public class SandBoxSecurityManager extends SecurityManager {

    private static final Logger LOGGER = LogManager.getLogger(SandBoxSecurityManager.class);

    static {
        //Yes it works SOMEHOW
        try {
            Field modifiers = Field.class.getDeclaredField("modifiers");
            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");

            modifiers.setAccessible(true);
            unsafe.setAccessible(true);

            modifiers.setInt(unsafe, unsafe.getModifiers() & ~Modifier.FINAL);
            unsafe.set(null, null);
        } catch (Exception e) {
            LOGGER.error("Can't disable unsafe");
            LOGGER.debug("Error", e);
        }
    }

    @Override
    public void checkPermission(Permission perm) {
        if (perm.implies(new RuntimePermission("setSecurityManager")) || perm.implies(new RuntimePermission("shutdownHooks")) || perm.getName().startsWith("getenv."))
            throw new RuntimeException();
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
        checkPermission(perm);
    }

    @Override
    public void checkLink(String lib) {
        if (!lib.equals("zip"))
            throw new RuntimeException(lib);
    }

    @Override
    public void checkCreateClassLoader() {
        throw new RuntimeException();
    }

    @Override
    public void checkAccess(Thread t) {
        throw new RuntimeException();
    }

    @Override
    public void checkAccess(ThreadGroup g) {
        throw new RuntimeException();
    }

    @Override
    public void checkSecurityAccess(String target) {
        throw new RuntimeException();
    }

    @Override
    public void checkExit(int status) {
        throw new RuntimeException();
    }

    @Override
    public void checkExec(String cmd) {
        throw new RuntimeException();
    }

    @Override
    public void checkDelete(String file) {
        throw new RuntimeException();
    }

    @Override
    public void checkConnect(String host, int port) {
        throw new RuntimeException();
    }

    @Override
    public void checkConnect(String host, int port, Object context) {
        throw new RuntimeException();
    }

    @Override
    public void checkListen(int port) {
        throw new RuntimeException();
    }

    @Override
    public void checkAccept(String host, int port) {
        throw new RuntimeException();
    }
}
