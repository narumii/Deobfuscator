package uwu.narumi.deobfuscator;

import uwu.narumi.deobfuscator.core.other.composed.ComposedGeneralFlowTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.PeepholeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineLocalVariablesTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineStaticFieldTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;
import uwu.narumi.deobfuscator.base.TestDeobfuscationBase;

import java.util.List;

public class TestDeobfuscation extends TestDeobfuscationBase {

  @Override
  protected void registerAll() {
    register("Inlining local variables", InputType.JAVA_CODE, List.of(
        InlineLocalVariablesTransformer::new,
        PeepholeCleanTransformer::new
    ), "TestInlineLocalVariables");
    register("Simple flow obfuscation", InputType.JAVA_CODE, List.of(ComposedGeneralFlowTransformer::new), "TestSimpleFlowObfuscation");
    register("Universal Number Transformer", InputType.JAVA_CODE, List.of(UniversalNumberTransformer::new), "TestUniversalNumberTransformer");
    // TODO: Uninitialized static fields should replace with 0?
    register("Inline static fields", InputType.JAVA_CODE, List.of(InlineStaticFieldTransformer::new), "TestInlineStaticFields");
    register("Inline static fields with modification", InputType.JAVA_CODE, List.of(InlineStaticFieldTransformer::new), "TestInlineStaticFieldsWithModification");

    // Samples
    register("Some flow obf sample", InputType.CUSTOM_CLASS, List.of(ComposedGeneralFlowTransformer::new), "FlowObfSample");
  }
}
