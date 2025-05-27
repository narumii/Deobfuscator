package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedGeneralFlowTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.LocalVariableNamesCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.unknown.obf1.UnknownObf1_StringByteArrayTransformer;

public class ComposedUnknownObf1Transformer extends ComposedTransformer {
  public ComposedUnknownObf1Transformer() {
    super(
        // Remove local variables names
        LocalVariableNamesCleanTransformer::new,

        // Fix flow
        ComposedGeneralFlowTransformer::new,

        // Decrypt strings
        UnknownObf1_StringByteArrayTransformer::new
    );
  }
}
