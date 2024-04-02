package uwu.narumi.deobfuscator.core.other.impl.universal;

import org.objectweb.asm.tree.LabelNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

// TODO: Will probably shit itself
public class TryCatchRepairTransformer extends Transformer {

  @Override
  public void transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).stream()
        .flatMap(classWrapper -> classWrapper.methods().stream())
        .forEach(
            methodNode -> {
              methodNode.tryCatchBlocks.removeIf(
                  tbce -> {
                    if (tbce.start.equals(tbce.end)
                        || tbce.start.equals(tbce.handler)
                        || tbce.end.equals(tbce.handler)) return true;

                    LabelNode start = tbce.start;
                    LabelNode handler = tbce.handler;
                    LabelNode end = tbce.end;

                    if (methodNode.instructions.indexOf(start) == -1
                        || methodNode.instructions.indexOf(handler) == -1
                        || methodNode.instructions.indexOf(end) == -1) return true;
                    else if (end.getNext() != null
                        && end.getNext().getNext() != null
                        && end.getNext().getOpcode() == ACONST_NULL
                        && end.getNext().getNext().getOpcode() == ATHROW) return true;
                    else
                      return methodNode.instructions.indexOf(start)
                              >= methodNode.instructions.indexOf(handler)
                          || methodNode.instructions.indexOf(start)
                              >= methodNode.instructions.indexOf(end)
                          || methodNode.instructions.indexOf(handler)
                              <= methodNode.instructions.indexOf(end);
                  });

              methodNode.exceptions.removeIf(
                  exception ->
                      methodNode.tryCatchBlocks.stream()
                          .noneMatch(tbce -> tbce.type.equals(exception)));
            });
  }
}
