package uwu.narumi.deobfuscator.transformer.impl.universal.other;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class StackOperationTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> classNode.methods.forEach(methodNode -> {
            transformNormally(methodNode);

            if (Arrays.stream(methodNode.instructions.toArray()).anyMatch(node -> node.getOpcode() >= POP && node.getOpcode() <= SWAP))
                transformUsingAnalyzer(classNode, methodNode);
        }));
    }

    //Idk if this works XD
    private void transformNormally(MethodNode methodNode) {
        boolean modified;
        do {
            modified = false;
            for (AbstractInsnNode node : methodNode.instructions.toArray()) {
                switch (node.getOpcode()) {
                    case POP: {
                        int type = check(node.getPrevious());
                        if (type == 1) {
                            methodNode.instructions.remove(node.getPrevious());
                            methodNode.instructions.remove(node);
                            modified = true;
                        }
                        break;
                    }
                    case POP2: {
                        int type = check(node.getPrevious());
                        if (type == 1 && check(node.getPrevious().getPrevious()) == 1) {
                            methodNode.instructions.remove(node.getPrevious().getPrevious());
                            methodNode.instructions.remove(node.getPrevious());
                            methodNode.instructions.remove(node);
                            modified = true;
                        } else if (type == 2) {
                            methodNode.instructions.remove(node.getPrevious());
                            methodNode.instructions.remove(node);
                            modified = true;
                        }
                        break;
                    }
                    case DUP: {
                        int type = check(node.getPrevious());
                        if (type == 1) {
                            methodNode.instructions.insert(node.getPrevious(), node.getPrevious().clone(null));
                            methodNode.instructions.remove(node);
                            modified = true;
                        }
                        break;
                    }
                    case DUP_X1: {
                        break;
                    }
                    case DUP_X2: {
                        break;
                    }
                    case DUP2: {
                        int type = check(node.getPrevious());
                        if (type == 2) {
                            methodNode.instructions.insert(node.getPrevious(), node.getPrevious().clone(null));
                            methodNode.instructions.remove(node);
                            modified = true;
                        }
                        break;
                    }
                    case DUP2_X1: {
                        break;
                    }
                    case DUP2_X2: {
                        break;
                    }
                    case SWAP: {
                        int firstType = check(node.getPrevious().getPrevious());
                        int secondType = check(node.getPrevious());

                        if (secondType == 1 && firstType == 1) {
                            AbstractInsnNode cloned = node.getPrevious().getPrevious();

                            methodNode.instructions.remove(node.getPrevious().getPrevious());
                            methodNode.instructions.set(node, cloned.clone(null));
                            modified = true;
                        }
                        break;
                    }
                }
            }
        } while (modified);
    }

    private void transformUsingAnalyzer(ClassNode classNode, MethodNode methodNode) {
        /*Map<AbstractInsnNode, Frame<SourceValue>> frames = analyzeSource(classNode, methodNode);

        if (frames == null)
            return;

        boolean modified;
        do {
            modified = false;
            for (AbstractInsnNode node : methodNode.instructions.toArray()) {
                Frame<SourceValue> frame = frames.get(node);

                if (frame == null)
                    continue;

                switch (node.getOpcode()) {
                    case POP: {
                        break;
                    }
                    case POP2: {
                    }
                    case DUP: {
                        break;
                    }
                    case DUP_X1: {
                        break;
                    }
                    case DUP_X2: {
                        break;
                    }
                    case DUP2: {
                        break;
                    }
                    case DUP2_X1: {
                        break;
                    }
                    case DUP2_X2: {
                        break;
                    }
                    case SWAP: {
                        break;
                    }
                }
            }
        } while (modified);*/
    }

    private int check(AbstractInsnNode node) {
        if (isLong(node) || isDouble(node)) {
            return 2;
        } else if (isInteger(node) || isFloat(node) || node instanceof LdcInsnNode) {
            return 1;
        }

        return 0;
    }
}
