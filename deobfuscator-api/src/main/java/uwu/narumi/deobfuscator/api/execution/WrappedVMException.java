package uwu.narumi.deobfuscator.api.execution;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.mirror.type.InstanceClass;
import dev.xdark.ssvm.operation.VMOperations;
import dev.xdark.ssvm.value.ArrayValue;
import dev.xdark.ssvm.value.InstanceValue;
import dev.xdark.ssvm.value.ObjectValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.IntStream;

/**
 * A wrapped {@link dev.xdark.ssvm.execution.VMException} that will print much prettier and readable exception
 */
public class WrappedVMException extends Throwable {
  private final String className;

  public WrappedVMException(String className, String message) {
    super(message);
    this.className = className;
  }

  @Override
  public String toString() {
    String s = this.className; // Set correct class name
    String message = getLocalizedMessage();
    return (message != null) ? (s + ": " + message) : s;
  }

  public static WrappedVMException wrap(InstanceValue exceptionInstance, VirtualMachine vm) {
    // Copied and modified from DefaultExceptionOperations#toJavaException
    VMOperations ops = vm.getOperations();
    // Exception message
    String msg = ops.readUtf8(ops.getReference(exceptionInstance, "detailMessage", "Ljava/lang/String;"));

    WrappedVMException wrappedVMException = new WrappedVMException(exceptionInstance.toString(), msg);

    // Get stacktrace
    ObjectValue backtrace = ops.getReference(exceptionInstance, "backtrace", "Ljava/lang/Object;");
    if (!backtrace.isNull()) {
      ArrayValue arrayValue = (ArrayValue) backtrace;
      StackTraceElement[] stackTrace = IntStream.range(0, arrayValue.getLength())
          .mapToObj(i -> {
            InstanceValue value = (InstanceValue) arrayValue.getReference(i);
            String declaringClass = ops.readUtf8(ops.getReference(value, "declaringClass", "Ljava/lang/String;"));
            String methodName = ops.readUtf8(ops.getReference(value, "methodName", "Ljava/lang/String;"));
            String fileName = ops.readUtf8(ops.getReference(value, "fileName", "Ljava/lang/String;"));
            int line = ops.getInt(value, "lineNumber");
            return new StackTraceElement(declaringClass, methodName, fileName, line);
          })
          .toArray(StackTraceElement[]::new);
      Collections.reverse(Arrays.asList(stackTrace));
      // Set stacktrace
      wrappedVMException.setStackTrace(stackTrace);
    }
    ObjectValue cause = ops.getReference(exceptionInstance, "cause", "Ljava/lang/Throwable;");
    if (!cause.isNull() && cause != exceptionInstance) {
      // Set cause
      wrappedVMException.initCause(wrap((InstanceValue) cause, vm));
    }

    // Init suppressed exceptions
    ObjectValue suppressedExceptions = ops.getReference(exceptionInstance, "suppressedExceptions", "Ljava/util/List;");
    if (!suppressedExceptions.isNull()) {
      InstanceClass cl = (InstanceClass) ops.findClass(vm.getMemoryManager().nullValue(), "java/util/ArrayList", false);
      if (cl == suppressedExceptions.getJavaClass()) {
        InstanceValue value = (InstanceValue) suppressedExceptions;
        int size = ops.getInt(value, "size");
        ArrayValue array = (ArrayValue) ops.getReference(value, "elementData", "[Ljava/lang/Object;");
        for (int i = 0; i < size; i++) {
          InstanceValue ref = (InstanceValue) array.getReference(i);
          wrappedVMException.addSuppressed(ref == exceptionInstance ? wrappedVMException : wrap(ref, vm));
        }
      }
    }

    return wrappedVMException;
  }
}
