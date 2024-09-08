package uwu.narumi.deobfuscator.transformer;

import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.execution.SandboxClassLoader;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.lang.reflect.Method;

public class TestSandboxSecurityTransformer extends Transformer {
  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    SandboxClassLoader sandboxClassLoader = new SandboxClassLoader(context);
    Class<?> clazz = Class.forName("TestSandboxSecurity", true, sandboxClassLoader);
    Method method = clazz.getDeclaredMethod("test");
    // Invoke test method
    method.invoke(null);
  }
}
