package uwu.narumi.deobfuscator.transformer.impl.colonial.r2;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

/**
 * @author Szymon on 28.03.2022
 * @project Deobfuscator
 */
public class ColonialBooleanTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes()
                .forEach(classNode -> {
                    classNode.methods.removeIf(methodNode -> methodNode.name.startsWith("ColonialObfuscator_"));
                    classNode.methods.forEach(methodNode ->
                            Arrays.stream(methodNode.instructions.toArray())
                                    .forEach(insnNode -> {
                                        if (isMethodStartWith(insnNode, classNode.name, "ColonialObfuscator_")
                                                && insnNode.getOpcode() == INVOKESTATIC) {
                                            methodNode.instructions.remove(insnNode);
                                        }
                                    }));
                });
    }
}
