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
import dev.xdark.ssvm.mirror.type.JavaClass;
import dev.xdark.ssvm.operation.VMOperations;
import dev.xdark.ssvm.symbol.Primitives;
import dev.xdark.ssvm.symbol.Symbols;
import dev.xdark.ssvm.thread.ThreadManager;
import dev.xdark.ssvm.util.Reflection;

import java.util.List;

import dev.xdark.ssvm.value.InstanceValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uwu.narumi.deobfuscator.api.context.Context;

/**
 * A wrapper for {@link VirtualMachine} with some additional features and patches
 */
public class SandBox {

  private static final Logger LOGGER = LogManager.getLogger(SandBox.class);

  private final VirtualMachine vm;
  private final SupplyingClassLoaderInstaller.DataSupplier dataSupplier;
  private final MemoryManager memoryManager;
  private final SupplyingClassLoaderInstaller.Helper helper;
  private final InvocationUtil invocationUtil;

  public SandBox(Context context) {
    // Install all classes from deobfuscator context
    this(new ClasspathDataSupplier(context.getClasspath()));
  }

  public SandBox(SupplyingClassLoaderInstaller.DataSupplier dataSupplier) {
    this(dataSupplier, new VirtualMachine());
  }

  public SandBox(SupplyingClassLoaderInstaller.DataSupplier dataSupplier, VirtualMachine vm) {
    LOGGER.info("Initializing SSVM sandbox...");
    this.dataSupplier = dataSupplier;
    this.vm = vm;

    try {
      this.vm.initialize();
      this.vm.bootstrap();
      this.memoryManager = vm.getMemoryManager();
      // Install all classes from data supplier
      this.helper = SupplyingClassLoaderInstaller.install(vm, this.dataSupplier);
      this.invocationUtil = InvocationUtil.create(vm);
      patchVm();
    } catch (Exception ex) {
      LOGGER.error("SSVM bootstrap failed. Make sure that you run this deobfuscator on java 17");
      if (ex instanceof VMException vmException) {
        SandBox.logVMException(vmException, vm);
      } else if (ex.getCause() instanceof VMException vmException) {
        SandBox.logVMException(vmException, vm);
      }

      throw new RuntimeException(ex);
    }
    LOGGER.info("Initialized SSVM sandbox");
  }

  private void patchVm() {
    // Some patches to circumvent bugs arising from VM implementation changes in later versions
    if (vm.getJvmVersion() > 8) {
      // Bug in SSVM makes it think there are overlapping sleeps, so until that gets fixed we stub
      // out sleeping.
      InstanceClass thread = vm.getSymbols().java_lang_Thread();
      vm.getInterface()
          .setInvoker(thread.getMethod("sleep", "(J)V"), MethodInvoker.noop());

      // SSVM manages its own memory, and this conflicts with it. Stubbing it out keeps everyone
      // happy.
      InstanceClass bits = (InstanceClass) vm.findBootstrapClass("java/nio/Bits");
      if (bits != null) {
        vm.getInterface()
            .setInvoker(bits.getMethod("reserveMemory", "(JJ)V"), MethodInvoker.noop());
      }
    }
  }

  /**
   * @see SandBox#logVMException(VMException, VirtualMachine)
   */
  public void logVMException(VMException ex) {
    logVMException(ex, this.vm);
  }

  /**
   * Converts {@link VMException} into readable java exception
   */
  public static void logVMException(VMException ex, VirtualMachine vm) {
    InstanceValue exceptionInstance = ex.getOop();

    // Print pretty exception
    LOGGER.error("VM thrown an exception", WrappedVMException.wrap(exceptionInstance, vm));
  }

  public VirtualMachine vm() {
    return vm;
  }

  public VMInterface getVMInterface() {
    return vm.getInterface();
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
    return vm.getSymbols();
  }

  public Primitives getPrimitives() {
    return vm.getPrimitives();
  }

  public VMOperations getOperations() {
    return vm.getOperations();
  }

  public LinkResolver getLinkResolver() {
    return vm.getLinkResolver();
  }

  public RuntimeResolver getRuntimeResolver() {
    return vm.getRuntimeResolver();
  }

  public Reflection getReflection() {
    return vm.getReflection();
  }

  public ThreadManager getThreadManager() {
    return vm.getThreadManager();
  }

  public FileManager getFileManager() {
    return vm.getFileManager();
  }

  public ExecutionEngine getExecutionEngine() {
    return vm.getExecutionEngine();
  }

  /**
   * Gets all classes from {@link Context} that were used by sandbox
   */
  public List<JavaClass> getUsedCustomClasses() {
    return this.vm.getClassStorage().list().stream()
        .filter(clazz -> this.dataSupplier.getClass(clazz.getName()) != null)
        .toList();
  }
}
