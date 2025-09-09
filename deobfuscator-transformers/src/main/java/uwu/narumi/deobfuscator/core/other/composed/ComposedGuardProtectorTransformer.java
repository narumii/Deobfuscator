package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedPeepholeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.guardprotector.GuardProtectorFlowTransformer;
import uwu.narumi.deobfuscator.core.other.impl.guardprotector.GuardProtectorNumberTransformer;
import uwu.narumi.deobfuscator.core.other.impl.guardprotector.GuardProtectorStringTransformer;

public class ComposedGuardProtectorTransformer extends ComposedTransformer {
  public ComposedGuardProtectorTransformer() {
    super(ComposedPeepholeCleanTransformer::new,
        GuardProtectorStringTransformer::new,
        GuardProtectorFlowTransformer::new,
        GuardProtectorNumberTransformer::new);
  }
}
