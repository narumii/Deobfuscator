package uwu.narumi.deobfuscator.transformer.impl.universal.other;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.helper.MathHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class UniversalNumberTransformer extends Transformer {

    private final boolean extended;

    public UniversalNumberTransformer() {
        this(true);
    }

    public UniversalNumberTransformer(boolean extended) {
        this.extended = extended;
    }

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> classNode.methods.forEach(methodNode -> transform(classNode, methodNode)));
    }

    public void transform(ClassNode classNode, MethodNode methodNode) {
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
                    } else if ((isLong(node.getPrevious().getPrevious()) && isInteger(node.getPrevious()))) {
                        long first = getLong(node.getPrevious().getPrevious());
                        long second = getInteger(node.getPrevious());

                        Long product = MathHelper.doMath(node.getOpcode(), first, second);
                        if (product != null) {
                            methodNode.instructions.remove(node.getPrevious().getPrevious());
                            methodNode.instructions.remove(node.getPrevious());
                            methodNode.instructions.set(node, getNumber(product));
                            modified = true;
                        }
                    } else if ((isInteger(node.getPrevious().getPrevious()) && isLong(node.getPrevious()))) {
                        long first = getInteger(node.getPrevious().getPrevious());
                        long second = getLong(node.getPrevious());

                        Long product = MathHelper.doMath(node.getOpcode(), first, second);
                        if (product != null) {
                            methodNode.instructions.remove(node.getPrevious().getPrevious());
                            methodNode.instructions.remove(node.getPrevious());
                            methodNode.instructions.set(node, getNumber(product));
                            modified = true;
                        }
                    }
                } else if (extended && (isLong(node) && isLong(node.getNext()) && node.getNext().getNext().getOpcode() == LCMP)) {
                    int result = Long.compare(getLong(node), getLong(node.getNext()));

                    methodNode.instructions.remove(node.getNext().getNext());
                    methodNode.instructions.remove(node.getNext());

                    methodNode.instructions.set(node, getNumber(result));
                } else if (extended && (node instanceof FieldInsnNode && ((FieldInsnNode) node).desc.equals("J") && node.getOpcode() == GETSTATIC
                        && isLong(node.getNext()) && node.getNext().getNext().getOpcode() == LCMP)) {

                    int result = Long.compare(getFieldValue(classNode, ((FieldInsnNode) node).name), getLong(node.getNext()));

                    methodNode.instructions.remove(node.getNext().getNext());
                    methodNode.instructions.remove(node.getNext());

                    methodNode.instructions.set(node, getNumber(result));
                }
            }
        } while (modified);
    }

    private long getFieldValue(ClassNode classNode, String name) {
        return classNode.fields.stream()
                .filter(fieldNode -> fieldNode.name.equals(name))
                .filter(fieldNode -> fieldNode.value != null)
                .map(fieldNode -> (long) fieldNode.value)
                .findFirst()
                .orElse(0L);
    }
}
