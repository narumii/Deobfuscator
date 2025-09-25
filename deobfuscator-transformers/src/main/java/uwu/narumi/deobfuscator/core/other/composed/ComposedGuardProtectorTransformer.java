package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedGeneralFlowTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedPeepholeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.guardprotector.GuardProtectorFlowTransformer;
import uwu.narumi.deobfuscator.core.other.impl.guardprotector.GuardProtectorNumberTransformer;
import uwu.narumi.deobfuscator.core.other.impl.guardprotector.GuardProtectorStringTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;

public class ComposedGuardProtectorTransformer extends ComposedTransformer {
  public ComposedGuardProtectorTransformer() {
    super(GuardProtectorStringTransformer::new,
        () -> new ComposedTransformer(true,
            ComposedPeepholeCleanTransformer::new,
            GuardProtectorFlowTransformer::new,
            ComposedGeneralFlowTransformer::new
        ),
        GuardProtectorNumberTransformer::new,
        UniversalNumberTransformer::new);
  }
}
