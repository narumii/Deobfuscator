package uwu.narumi.deobfuscator.transformer.impl.qprotect.b3_0;

import org.objectweb.asm.tree.AbstractInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

/*
    This transformer works only on version: 3.0-b1
 */
public class qProtectStackOperationTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    boolean modified;
                    do {
                        modified = false;

                        for (AbstractInsnNode node : methodNode.instructions.toArray()) {
                            if (isInteger(node) && isInteger(node.getNext())
                                    && node.getNext().getNext() != null
                                    && node.getNext().getNext().getOpcode() == SWAP
                                    && node.getNext().getNext().getNext() != null
                                    && node.getNext().getNext().getNext().getOpcode() == DUP_X1
                                    && node.getNext().getNext().getNext().getNext() != null
                                    && node.getNext().getNext().getNext().getNext().getOpcode() == POP2) {

                                methodNode.instructions.remove(node.getNext().getNext().getNext().getNext());
                                methodNode.instructions.remove(node.getNext().getNext().getNext());
                                methodNode.instructions.remove(node.getNext().getNext());
                                methodNode.instructions.remove(node.getNext());
                                modified = true;
                            }
                        }
                    } while (modified);
                });
    }
}
