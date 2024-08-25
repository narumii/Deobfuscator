package uwu.narumii.deobfuscator;

import uwu.narumi.deobfuscator.core.other.composed.ComposedGeneralFlowTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.PeepholeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineLocalVariablesTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineStaticFieldTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;
import uwu.narumii.deobfuscator.base.TestDeobfuscationBase;

import java.util.List;

public class TestDeobfuscation extends TestDeobfuscationBase {

  @Override
  protected void registerAll() {
    register("Inlining local variables", InputType.JAVA_CODE, List.of(
        InlineLocalVariablesTransformer::new,
        PeepholeCleanTransformer::new
    ), "TestInlineLocalVariables");
    // TODO: Deobfuscate useless tricky while loops
    register("Simple flow obfuscation", InputType.JAVA_CODE, List.of(ComposedGeneralFlowTransformer::new), "TestSimpleFlowObfuscation");
    register("Universal Number Transformer", InputType.JAVA_CODE, List.of(UniversalNumberTransformer::new), "TestUniversalNumberTransformer");
    register("Inline static fields", InputType.JAVA_CODE, List.of(InlineStaticFieldTransformer::new), "TestInlineStaticFields");

    // Samples
    // TODO: Deobfuscate switches
    register("Some flow obf sample", InputType.CUSTOM_CLASS, List.of(ComposedGeneralFlowTransformer::new), "FlowObfSample");
  }
}
