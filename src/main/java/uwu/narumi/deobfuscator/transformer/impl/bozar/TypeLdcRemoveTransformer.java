package uwu.narumi.deobfuscator.transformer.impl.bozar;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LdcInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class TypeLdcRemoveTransformer extends Transformer {

    /*
    Some remains remover
     */
    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                        .filter(node -> node instanceof LdcInsnNode)
                        .map(LdcInsnNode.class::cast)
                        .filter(node -> node.cst instanceof Type)
                        .filter(node -> ((Type) node.cst).getDescriptor().equals("()Z"))
                        .forEach(node -> {
                            methodNode.instructions.remove(node.getNext());
                            methodNode.instructions.remove(node);
                        })
                );
    }
}
