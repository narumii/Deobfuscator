package uwu.narumi.deobfuscator.transformer.impl.sb27;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.MathHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

/*
   Who knows maybe in near future Superblaubeere is going to add float and double obfuscation?
 */
public class SuperblaubeereNumberTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    boolean modified;
                    do {
                        modified = false;

                        for (AbstractInsnNode node : methodNode.instructions.toArray()) {
                            if (isString(node)
                                    && node.getNext() instanceof MethodInsnNode
                                    && ((MethodInsnNode) node.getNext()).name.equals("length")
                                    && ((MethodInsnNode) node.getNext()).owner.equals("java/lang/String")) {

                                methodNode.instructions.remove(node.getNext());
                                methodNode.instructions.set(node, getNumber(getString(node).length()));
                                modified = true;
                            } else if (node.getOpcode() == INEG || node.getOpcode() == LNEG) {
                                if (isInteger(node.getPrevious())) {
                                    int number = -getInteger(node.getPrevious());

                                    methodNode.instructions.remove(node.getPrevious());
                                    methodNode.instructions.set(node, getNumber(number));
                                    modified = true;
                                } else if (isLong(node.getPrevious())) {
                                    long number = -getLong(node.getPrevious());

                                    methodNode.instructions.remove(node.getPrevious());
                                    methodNode.instructions.set(node, getNumber(number));
                                    modified = true;
                                }
                            } else if ((node.getOpcode() >= IADD && node.getOpcode() <= LXOR)) {
                                if (isInteger(node.getPrevious().getPrevious()) && isInteger(node.getPrevious())) {
                                    int first = getInteger(node.getPrevious().getPrevious());
                                    int second = getInteger(node.getPrevious());

                                    Integer product = MathHelper.doMath(node.getOpcode(), first, second);
                                    if (product != null) {
                                        methodNode.instructions.remove(node.getPrevious().getPrevious());
                                        methodNode.instructions.remove(node.getPrevious());
                                        methodNode.instructions.set(node, getNumber(product));
                                        modified = true;
                                    }
                                } else if (isLong(node.getPrevious().getPrevious()) && isLong(node.getPrevious())) {
                                    long first = getLong(node.getPrevious().getPrevious());
                                    long second = getLong(node.getPrevious());

                                    Long product = MathHelper.doMath(node.getOpcode(), first, second);
                                    if (product != null) {
                                        methodNode.instructions.remove(node.getPrevious().getPrevious());
                                        methodNode.instructions.remove(node.getPrevious());
                                        methodNode.instructions.set(node, getNumber(product));
                                        modified = true;
                                    }
                                }
                            }
                        }
                    } while (modified);
                });
    }
}
