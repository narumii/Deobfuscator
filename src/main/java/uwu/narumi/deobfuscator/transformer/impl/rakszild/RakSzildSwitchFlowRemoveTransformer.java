package uwu.narumi.deobfuscator.transformer.impl.rakszild;

import org.objectweb.asm.tree.LookupSwitchInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class RakSzildSwitchFlowRemoveTransformer extends Transformer {

    /*
    Gdy myslisz ze twoj flow obf jest dobry ale wystarczy wyjebac 2 instrukcje aby znikl XD
     */
    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                        .filter(node -> node instanceof LookupSwitchInsnNode)
                        .filter(node -> isInteger(node.getPrevious()))
                        .map(LookupSwitchInsnNode.class::cast)
                        .filter(node -> node.keys == null || node.keys.isEmpty())
                        .forEach(node -> {
                            methodNode.instructions.remove(node.getPrevious());
                            methodNode.instructions.remove(node);
                        }));
    }
}
