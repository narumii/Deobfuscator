package uwu.narumi.deobfuscator.transformer.impl.universal.other;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class UnHideTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            if (isAccess(classNode.access, ACC_SYNTHETIC)) {
                classNode.access &= ~ACC_SYNTHETIC;
            }

            classNode.fields.stream()
                    .filter(node -> isAccess(node.access, ACC_SYNTHETIC))
                    .forEach(node -> node.access &= ~ACC_SYNTHETIC);

            classNode.methods.forEach(methodNode -> {
                if (isAccess(methodNode.access, ACC_SYNTHETIC)) {
                    methodNode.access &= ~ACC_SYNTHETIC;
                }

                if (isAccess(methodNode.access, ACC_BRIDGE)) {
                    methodNode.access &= ~ACC_BRIDGE;
                }
            });
        });
    }
}
