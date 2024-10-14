package uwu.narumi.deobfuscator.transformer;

import dev.xdark.ssvm.execution.VMException;
import dev.xdark.ssvm.mirror.type.InstanceClass;
import uwu.narumi.deobfuscator.api.execution.SandBox;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestSandboxSecurityTransformer extends Transformer {
  @Override
  protected void transform() throws Exception {
    SandBox sandBox = context().getSandBox();
    InstanceClass clazz = sandBox.getHelper().loadClass("sandbox.TestSandboxSecurity");

    assertThrows(VMException.class, () -> {
      sandBox.getInvocationUtil().invokeInt(
          clazz.getMethod("test", "()I")
      );
    });
  }
}
