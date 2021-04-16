package uwu.narumi;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.impl.AntiDebugRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.BadAnnotationRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.FlowRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.SignatureRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.TrashCodeRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.TrashExceptionRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.TrashInstructionsRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.TrashLabelsRemoveTransformer;
import uwu.narumi.deobfuscator.transformer.impl.UnHideCodeTransformer;

public class Bootstrap {

  public static void main(String... args) throws Exception {
    Deobfuscator.builder()
        .input("example/input-trashcode.jar")
        .output("example/output-trashcode.jar")
        .with(
            new AntiDebugRemoveTransformer(),
            new BadAnnotationRemoveTransformer(),
            new FlowRemoveTransformer(),
            new SignatureRemoveTransformer(),
            new TrashCodeRemoveTransformer(),
            new TrashExceptionRemoveTransformer(),
            new TrashLabelsRemoveTransformer(),
            new TrashInstructionsRemoveTransformer(),
            new UnHideCodeTransformer()
        )
        .build()
        .start();
  }
}
