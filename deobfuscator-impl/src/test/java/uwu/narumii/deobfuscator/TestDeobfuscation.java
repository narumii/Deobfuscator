package uwu.narumii.deobfuscator;

import uwu.narumi.deobfuscator.core.other.composed.ComposedGeneralFlowTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineLocalVariablesTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalFlowTransformer;
import uwu.narumii.deobfuscator.base.TestDeobfuscationBase;

import java.util.List;

public class TestDeobfuscation extends TestDeobfuscationBase {

  @Override
  protected void registerAll() {
    // Register your tests here
    register("Inlining local variables", InputType.JAVA_CODE, List.of(InlineLocalVariablesTransformer::new), "TestInlineLocalVariables");
    register("Simple flow obfuscation", InputType.JAVA_CODE, List.of(ComposedGeneralFlowTransformer::new), "TestSimpleFlowObfuscation");

    // Samples
    // TODO: Deobfuscate switches
    register("Some flow obf sample", InputType.CUSTOM_CLASS, List.of(ComposedGeneralFlowTransformer::new), "FlowObfSample");
  }
}
