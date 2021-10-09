package uwu.narumi.deobfuscator.transformer.impl.qprotect.idk;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

/*
    I don't know for what version this is i had some jar on my computer but it has only flow
 */
public class qProtectFlowTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        firstPhase(deobfuscator);
        secondPhase(deobfuscator);
    }

    private void firstPhase(Deobfuscator deobfuscator) {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    boolean modified;
                    do {
                        modified = false;
                        for (AbstractInsnNode node : methodNode.instructions.toArray()) {
                            if (node.getOpcode() == GOTO && node.getNext() instanceof LabelNode && node.getNext().equals(((JumpInsnNode) node).label)) {
                                methodNode.instructions.remove(node.getNext());
                                methodNode.instructions.remove(node);
                                modified = true;
                            } else if (node instanceof LookupSwitchInsnNode && isInteger(node.getPrevious())) {
                                int key = getInteger(node.getPrevious());
                                LookupSwitchInsnNode switchNode = (LookupSwitchInsnNode) node;
                                LabelNode originalCodeBlockStart = switchNode.keys.contains(key) ? switchNode.labels.get(switchNode.keys.indexOf(key)) : switchNode.dflt;

                                if (originalCodeBlockStart.getNext().getOpcode() == GOTO) {
                                    methodNode.instructions.remove(node.getPrevious());
                                    methodNode.instructions.set(node, new JumpInsnNode(GOTO, ((JumpInsnNode) originalCodeBlockStart.getNext()).label));
                                    modified = true;
                                }
                            } else if (node instanceof TableSwitchInsnNode && isInteger(node.getPrevious())) {
                                int key = getInteger(node.getPrevious());
                                TableSwitchInsnNode switchNode = (TableSwitchInsnNode) node;
                                LabelNode originalCodeBlockStart = key < switchNode.min || key > switchNode.max ? switchNode.dflt : switchNode.labels.get(key);

                                methodNode.instructions.remove(node.getPrevious());
                                methodNode.instructions.set(node, new JumpInsnNode(GOTO, originalCodeBlockStart));
                                modified = true;
                            }
                        }
                    } while (modified);
                });
    }

    private void secondPhase(Deobfuscator deobfuscator) {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    boolean modified;
                    do {
                        modified = false;
                        for (AbstractInsnNode node : methodNode.instructions.toArray()) {
                            if (!(node instanceof JumpInsnNode && node.getPrevious() instanceof FieldInsnNode && node.getPrevious().getOpcode() == GETSTATIC))
                                continue;

                            if (node.getNext().getOpcode() == ACONST_NULL && node.getNext().getNext().getOpcode() == ATHROW) {
                                if (node.getPrevious().getPrevious().getOpcode() == GETSTATIC) {
                                    methodNode.instructions.remove(node.getPrevious().getPrevious());
                                }
                                methodNode.instructions.remove(node.getPrevious());
                                methodNode.instructions.set(node, new JumpInsnNode(GOTO, ((JumpInsnNode) node).label));
                                modified = true;
                            }
                        }
                    } while (modified);
                });
    }
}
