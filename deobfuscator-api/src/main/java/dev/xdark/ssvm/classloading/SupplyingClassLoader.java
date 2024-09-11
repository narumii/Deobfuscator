package dev.xdark.ssvm.classloading;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.api.MethodInvoker;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.mirror.member.JavaMethod;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * A custom classloader to supply additional content to the {@link VirtualMachine}.
 * <br>
 * You will want to install this into the VM and use {@link VMInterface#setInvoker(JavaMethod, MethodInvoker)} on the
 * providing methods to supply classes and resources.
 *
 * @see SupplyingClassLoaderInstaller
 *
 * @author Matt Coley
 */
public class SupplyingClassLoader extends ClassLoader {
  public native byte[] provideResource(String name);

  public native byte[] provideClass(String name);

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    byte[] classBytes = provideClass(name);
    if (classBytes != null)
      return defineClass(name, classBytes, 0, classBytes.length);
    return super.findClass(name);
  }

  @Override
  protected URL findResource(String name) {
    byte[] resourceBytes = provideResource(name);
    if (resourceBytes != null) {
      try {
        return new URL("memory", "", -1, "", new URLStreamHandler() {
          @Override
          protected URLConnection openConnection(URL u) {
            return new URLConnection(u) {
              private InputStream is;

              @Override
              public void connect() {
                // no-op
              }

              @Override
              public InputStream getInputStream() {
                if (is == null) {
                  is = new ByteArrayInputStream(resourceBytes);
                }
                return is;
              }
            };
          }
        });
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }

    return super.findResource(name);
  }
}
