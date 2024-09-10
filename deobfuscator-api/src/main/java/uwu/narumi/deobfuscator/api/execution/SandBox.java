package uwu.narumi.deobfuscator.api.execution;

import dev.xdark.ssvm.LinkResolver;
import dev.xdark.ssvm.RuntimeResolver;
import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.api.MethodInvoker;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.classloading.SupplyingClassLoaderInstaller;
import dev.xdark.ssvm.execution.ExecutionEngine;
import dev.xdark.ssvm.execution.VMException;
import dev.xdark.ssvm.filesystem.FileManager;
import dev.xdark.ssvm.invoke.InvocationUtil;
import dev.xdark.ssvm.memory.management.MemoryManager;
import dev.xdark.ssvm.mirror.type.InstanceClass;
import dev.xdark.ssvm.operation.VMOperations;
import dev.xdark.ssvm.symbol.Primitives;
import dev.xdark.ssvm.symbol.Symbols;
import dev.xdark.ssvm.thread.ThreadManager;
import dev.xdark.ssvm.util.Reflection;
import java.io.PrintWriter;
import java.io.StringWriter;

import dev.xdark.ssvm.value.InstanceValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uwu.narumi.deobfuscator.api.library.LibraryClassLoader;

public class SandBox {

  private static final Logger LOGGER = LogManager.getLogger(SandBox.class);

  private final LibraryClassLoader loader;

  private final VirtualMachine virtualMachine;
  private final MemoryManager memoryManager;
  private final SupplyingClassLoaderInstaller.Helper helper;
  private final InvocationUtil invocationUtil;

  public SandBox(LibraryClassLoader loader) {
    this(loader, new VirtualMachine());
  }

  public SandBox(LibraryClassLoader loader, VirtualMachine virtualMachine) {
    this.loader = loader;
    this.virtualMachine = virtualMachine;

    try {
      this.virtualMachine.initialize();
      this.virtualMachine.bootstrap();
      this.memoryManager = virtualMachine.getMemoryManager();
      // Install all classes from deobfuscator context
      this.helper = SupplyingClassLoaderInstaller.install(virtualMachine, new ClassLoaderDataSupplier(loader));
      this.invocationUtil = InvocationUtil.create(virtualMachine);
      patchVm();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void patchVm() {
    // Some patches to circumvent bugs arising from VM implementation changes in later versions
    if (virtualMachine.getJvmVersion() > 8) {
      // Bug in SSVM makes it think there are overlapping sleeps, so until that gets fixed we stub
      // out sleeping.
      InstanceClass thread = virtualMachine.getSymbols().java_lang_Thread();
      virtualMachine
          .getInterface()
          .setInvoker(thread.getMethod("sleep", "(J)V"), MethodInvoker.noop());

      // SSVM manages its own memory, and this conflicts with it. Stubbing it out keeps everyone
      // happy.
      InstanceClass bits = (InstanceClass) virtualMachine.findBootstrapClass("java/nio/Bits");
      if (bits != null) {
        virtualMachine
            .getInterface()
            .setInvoker(bits.getMethod("reserveMemory", "(JJ)V"), MethodInvoker.noop());
      }
    }
  }

  public static String toString(Throwable t) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    t.printStackTrace(printWriter);
    return stringWriter.toString();
  }

  /**
   * Converts {@link VMException} into readable java exception
   */
  public void handleVMException(VMException ex) {
    InstanceValue oop = ex.getOop();
    if (oop.getJavaClass() == virtualMachine.getSymbols().java_lang_ExceptionInInitializerError()) {
      oop = (InstanceValue) virtualMachine.getOperations().getReference(oop, "exception", "Ljava/lang/Throwable;");
    }

    // Print pretty exception
    LOGGER.error(oop);
    LOGGER.error(virtualMachine.getOperations().toJavaException(oop));
    throw ex;
  }

  public VirtualMachine getVirtualMachine() {
    return virtualMachine;
  }

  public VMInterface getVMInterface() {
    return virtualMachine.getInterface();
  }

  public MemoryManager getMemoryManager() {
    return memoryManager;
  }

  public SupplyingClassLoaderInstaller.Helper getHelper() {
    return helper;
  }

  public InvocationUtil getInvocationUtil() {
    return invocationUtil;
  }

  public Symbols getSymbols() {
    return virtualMachine.getSymbols();
  }

  public Primitives getPrimitives() {
    return virtualMachine.getPrimitives();
  }

  public VMOperations getOperations() {
    return virtualMachine.getOperations();
  }

  public LinkResolver getLinkResolver() {
    return virtualMachine.getLinkResolver();
  }

  public RuntimeResolver getRuntimeResolver() {
    return virtualMachine.getRuntimeResolver();
  }

  public Reflection getReflection() {
    return virtualMachine.getReflection();
  }

  public ThreadManager getThreadManager() {
    return virtualMachine.getThreadManager();
  }

  public FileManager getFileManager() {
    return virtualMachine.getFileManager();
  }

  public ExecutionEngine getExecutionEngine() {
    return virtualMachine.getExecutionEngine();
  }
}
