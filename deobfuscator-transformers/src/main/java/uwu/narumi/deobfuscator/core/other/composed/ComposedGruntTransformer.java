package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.composed.general.ComposedGeneralFlowTransformer;
import uwu.narumi.deobfuscator.core.other.impl.grunt.GruntConstantPoolTransformer;
import uwu.narumi.deobfuscator.core.other.impl.grunt.GruntInvokeDynamicTransformer;
import uwu.narumi.deobfuscator.core.other.impl.grunt.GruntStringTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineStaticFieldTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.AccessRepairTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.InlinePureFunctionsTransformer;

/**
 * <a href="https://github.com/SpartanB312/Grunt">https://github.com/SpartanB312/Grunt</a>
 * This was tested for Grunt 2.x (Gruntpocalypse), it may not work on newer versions.
 */
public class ComposedGruntTransformer extends ComposedTransformer {
  public ComposedGruntTransformer() {
    super(
        // Repair access
        AccessRepairTransformer::new,
        () -> new ComposedTransformer(true,
            // Fix flow
            ComposedGeneralFlowTransformer::new,
            InlineStaticFieldTransformer::new,
            // Inline pure functions
            InlinePureFunctionsTransformer::new
        ),
        // Fix invoke dynamics (BEFORE decrypting strings)
        GruntInvokeDynamicTransformer::new,
        // Decrypt strings
        GruntStringTransformer::new
        // Decrypt constant pool
//        GruntConstantPoolTransformer::new
    );
  }
}
