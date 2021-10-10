package uwu.narumi.deobfuscator.transformer.impl.caesium;

import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.MathHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

/*
    Buggy as fuck
 */
public class CaesiumCleanTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            classNode.fields.removeIf(fieldNode -> (fieldNode.desc.equals("J") || fieldNode.desc.equals("I")) &&
                    MathHelper.INTEGER_PATTERN.matcher(fieldNode.name).matches());

            findClInit(classNode).ifPresent(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node instanceof MethodInsnNode)
                    .filter(node -> node.getOpcode() == INVOKESTATIC)
                    .map(MethodInsnNode.class::cast)
                    .filter(node -> MathHelper.INTEGER_PATTERN.matcher(node.name).matches())
                    .filter(node -> node.desc.equals("()V"))
                    .forEach(methodNode.instructions::remove));

            classNode.methods.removeIf(methodNode -> methodNode.desc.equals("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/Object;") &&
                    MathHelper.INTEGER_PATTERN.matcher(methodNode.name).matches());
        });
    }
}
