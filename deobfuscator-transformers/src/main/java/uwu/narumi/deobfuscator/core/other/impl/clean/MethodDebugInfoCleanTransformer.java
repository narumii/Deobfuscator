package uwu.narumi.deobfuscator.core.other.impl.clean;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;

public class MethodDebugInfoCleanTransformer extends ComposedTransformer {
  public MethodDebugInfoCleanTransformer() {
    super(
        LocalVariableNamesCleanTransformer::new,
        ParametersInfoCleanTransformer::new,
        ThrowsExceptionCleanTransformer::new
    );
  }
}
