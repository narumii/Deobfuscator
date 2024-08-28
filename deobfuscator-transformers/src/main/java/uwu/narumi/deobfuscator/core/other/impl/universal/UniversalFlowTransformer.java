package uwu.narumi.deobfuscator.core.other.impl.universal;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.flow.JumpFlowTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.flow.SwitchFlowTransformer;

public class UniversalFlowTransformer extends ComposedTransformer {
  public UniversalFlowTransformer() {
    super(
        JumpFlowTransformer::new,
        SwitchFlowTransformer::new
    );
  }
}
