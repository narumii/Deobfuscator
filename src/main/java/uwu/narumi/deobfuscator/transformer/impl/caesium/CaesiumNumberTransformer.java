package uwu.narumi.deobfuscator.transformer.impl.caesium;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class CaesiumNumberTransformer extends Transformer {

    /*
     TODO: Cleanup this code looks like shit
     */

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    boolean modified;
                    do {
                        modified = false;

                        for (AbstractInsnNode node : methodNode.instructions.toArray()) {
                            if (node instanceof MethodInsnNode
                                    && ((MethodInsnNode) node).name.equals("reverse")
                                    && ((MethodInsnNode) node).owner.equals("java/lang/Integer")
                                    && ((MethodInsnNode) node).desc.equals("(I)I")
                                    && isInteger(node.getPrevious())) {

                                int number = Integer.reverse(getInteger(node.getPrevious()));
                                methodNode.instructions.remove(node.getPrevious());
                                methodNode.instructions.set(node, getNumber(number));
                                modified = true;
                            } else if (node instanceof MethodInsnNode
                                    && ((MethodInsnNode) node).name.equals("reverse")
                                    && ((MethodInsnNode) node).owner.equals("java/lang/Long")
                                    && ((MethodInsnNode) node).desc.equals("(J)J")
                                    && isLong(node.getPrevious())) {

                                long number = Long.reverse(getLong(node.getPrevious()));
                                methodNode.instructions.remove(node.getPrevious());
                                methodNode.instructions.set(node, getNumber(number));
                                modified = true;
                            } else if (node instanceof MethodInsnNode
                                    && ((MethodInsnNode) node).name.equals("floatToIntBits")
                                    && ((MethodInsnNode) node).owner.equals("java/lang/Float")
                                    && ((MethodInsnNode) node).desc.equals("(F)I")
                                    && isFloat(node.getPrevious()) && isInteger(node.getNext()) && (node.getNext().getNext().getOpcode() >= IADD && node.getNext().getNext().getOpcode() <= LXOR)) {

                                float number = Float.floatToIntBits(getFloat(node.getPrevious()));
                                methodNode.instructions.remove(node.getPrevious());
                                methodNode.instructions.set(node, getNumber(number));
                                modified = true;
                            } else if (node instanceof MethodInsnNode
                                    && ((MethodInsnNode) node).name.equals("doubleToLongBits")
                                    && ((MethodInsnNode) node).owner.equals("java/lang/Double")
                                    && ((MethodInsnNode) node).desc.equals("(D)J")
                                    && isDouble(node.getPrevious()) && isLong(node.getNext()) && (node.getNext().getNext().getOpcode() >= IADD && node.getNext().getNext().getOpcode() <= LXOR)) {

                                long number = Double.doubleToLongBits(getDouble(node.getPrevious()));
                                methodNode.instructions.remove(node.getPrevious());
                                methodNode.instructions.set(node, getNumber(number));
                                modified = true;
                            } else if (node instanceof MethodInsnNode
                                    && ((MethodInsnNode) node).name.equals("intBitsToFloat")
                                    && ((MethodInsnNode) node).owner.equals("java/lang/Float")
                                    && ((MethodInsnNode) node).desc.equals("(I)F")
                                    && isInteger(node.getPrevious())) {

                                float number = Float.intBitsToFloat(getInteger(node.getPrevious()));
                                methodNode.instructions.remove(node.getPrevious());
                                methodNode.instructions.set(node, getNumber(number));
                                modified = true;
                            } else if (node instanceof MethodInsnNode
                                    && ((MethodInsnNode) node).name.equals("longBitsToDouble")
                                    && ((MethodInsnNode) node).owner.equals("java/lang/Double")
                                    && ((MethodInsnNode) node).desc.equals("(J)D")
                                    && isLong(node.getPrevious())) {

                                double number = Double.longBitsToDouble(getLong(node.getPrevious()));
                                methodNode.instructions.remove(node.getPrevious());
                                methodNode.instructions.set(node, getNumber(number));
                                modified = true;
                            }
                        }
                    } while (modified);
                });
    }
}
