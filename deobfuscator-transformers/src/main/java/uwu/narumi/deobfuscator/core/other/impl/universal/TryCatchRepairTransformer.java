package uwu.narumi.deobfuscator.core.other.impl.universal;

import org.objectweb.asm.tree.LabelNode;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class TryCatchRepairTransformer extends Transformer {

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      methodNode.tryCatchBlocks.removeIf(tryCatchBlock -> {
        LabelNode start = tryCatchBlock.start;
        LabelNode handler = tryCatchBlock.handler;
        LabelNode end = tryCatchBlock.end;

        if (start.equals(end) || start.equals(handler) || end.equals(handler)) {
          // Try-catch has overlapping labels. Remove it.
          markChange();
          return true;
        }

        // Check if try-catch labels exist
        if (methodNode.instructions.indexOf(start) == -1 || methodNode.instructions.indexOf(handler) == -1 || methodNode.instructions.indexOf(end) == -1) {
          return true;
        }

        // Check if try-catch labels are in the correct order
        return methodNode.instructions.indexOf(start) >= methodNode.instructions.indexOf(handler)
            || methodNode.instructions.indexOf(start) >= methodNode.instructions.indexOf(end)
            || methodNode.instructions.indexOf(handler) <= methodNode.instructions.indexOf(end);
      });

      // Remove exceptions that are already caught by try-catch blocks
      methodNode.exceptions.removeIf(exception ->
          methodNode.tryCatchBlocks.stream().noneMatch(tryCatch -> tryCatch.type != null && tryCatch.type.equals(exception))
      );
    }));
  }
}
