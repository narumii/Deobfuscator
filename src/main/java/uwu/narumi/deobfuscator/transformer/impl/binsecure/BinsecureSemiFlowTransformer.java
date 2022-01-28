package uwu.narumi.deobfuscator.transformer.impl.binsecure;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

/*
    Working in 80% i think
    TODO: Better switch remove
    TODO: Fucking method flow ofbustation bruh
    TODO: While loop remove
 */
public class BinsecureSemiFlowTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        firstPhase(deobfuscator);
        secondPhase(deobfuscator);
        thirdPhase(deobfuscator);
        fourthPhase(deobfuscator);
        fifthPhase(deobfuscator);
        clean(deobfuscator);
    }

    private void firstPhase(Deobfuscator deobfuscator) {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    boolean modified;
                    do {
                        modified = false;

                        for (AbstractInsnNode node : methodNode.instructions.toArray()) {
                            if (node instanceof JumpInsnNode && node.getNext() != null && node.getNext().equals(((JumpInsnNode) node).label)) {
                                methodNode.instructions.remove(node);
                                modified = true;
                            } else if (node instanceof JumpInsnNode && node.getPrevious() != null && node.getPrevious().equals(((JumpInsnNode) node).label)) {
                                methodNode.instructions.remove(node);
                                modified = true;
                            } else if (node.getOpcode() == ATHROW && node.getPrevious() instanceof LabelNode && node.getNext() instanceof LabelNode && node.getPrevious().getPrevious() != null && node.getPrevious().getPrevious().getOpcode() == GOTO) {
                                methodNode.instructions.remove(node.getPrevious().getPrevious());
                                methodNode.instructions.remove(node);
                                modified = true;
                            }
                        }
                    } while (modified);
                });
    }

    private void secondPhase(Deobfuscator deobfuscator) {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                        .filter(node -> node instanceof LookupSwitchInsnNode)
                        .filter(node -> node.getPrevious() != null)
                        .filter(node -> node.getPrevious().getOpcode() == IXOR)
                        .filter(node -> isInteger(node.getPrevious().getPrevious()))
                        .map(LookupSwitchInsnNode.class::cast)
                        .forEach(node -> {
                            AbstractInsnNode fieldInsn = node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious();
                            AbstractInsnNode jumpInsn = node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious();
                            AbstractInsnNode thirdNumberInsn = node.getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious().getPrevious();
                            AbstractInsnNode secondNumberInsn = node.getPrevious().getPrevious().getPrevious().getPrevious();
                            AbstractInsnNode firstNumberInsn = node.getPrevious().getPrevious();
                            if (!(isInteger(firstNumberInsn) && isInteger(secondNumberInsn) && isInteger(thirdNumberInsn) /*&& jumpInsn instanceof JumpInsnNode && fieldInsn.getOpcode() == GETSTATIC*/))
                                return;

                            //field number is always 1 xd
                            int third = getInteger(thirdNumberInsn);
                            int second = getInteger(secondNumberInsn);
                            int xor = getInteger(firstNumberInsn);

                            Integer numberToXor = null;
                            switch (jumpInsn.getOpcode()) {
                                case IFGT:
                                case IFGE: {
                                    numberToXor = second;
                                    break;
                                }
                                case IFLE:
                                case IFNE:
                                case IFEQ:
                                case IFLT: {
                                    numberToXor = third;
                                    break;
                                }
                            }

                            if (numberToXor == null)
                                return;

                            int number = numberToXor ^ xor;

                            getInstructionsBetween(fieldInsn, node, true, false).stream()
                                    .filter(insn -> !(insn instanceof LabelNode))
                                    .forEach(methodNode.instructions::remove);

                            methodNode.instructions.insertBefore(node, getNumber(number));
                        }));
    }

    private void thirdPhase(Deobfuscator deobfuscator) {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    for (AbstractInsnNode node : methodNode.instructions.toArray()) {
                        if (!(node instanceof LookupSwitchInsnNode && isInteger(node.getPrevious())))
                            continue;

                        int key = getInteger(node.getPrevious());
                        LookupSwitchInsnNode switchNode = (LookupSwitchInsnNode) node;
                        LabelNode originalCodeBlockStart = switchNode.keys.contains(key) ? switchNode.labels.get(switchNode.keys.indexOf(key)) : switchNode.dflt;

                        switchNode.labels.stream()
                                .filter(labelNode -> !labelNode.equals(originalCodeBlockStart))
                                .filter(labelNode -> methodNode.instructions.indexOf(labelNode) < methodNode.instructions.indexOf(switchNode))
                                .forEach(labelNode -> {
                                    AbstractInsnNode codeBlockEnd = labelNode;

                                    {
                                        while (codeBlockEnd.equals(labelNode) || !(codeBlockEnd instanceof LabelNode)) {
                                            if (codeBlockEnd.getNext() == null) {
                                                break;
                                            }

                                            codeBlockEnd = codeBlockEnd.getNext();
                                        }
                                    }

                                    boolean canExtract = codeBlockEnd instanceof LabelNode || (codeBlockEnd.getOpcode() == ATHROW && codeBlockEnd.getPrevious().getOpcode() == ACONST_NULL);
                                    if (!canExtract)
                                        return;

                                    int codeStart = methodNode.instructions.indexOf(labelNode) + 1;
                                    int codeEnd = methodNode.instructions.indexOf(codeBlockEnd);

                                    if (codeBlockEnd instanceof LabelNode)
                                        codeEnd--;

                                    AbstractInsnNode[] nodes = methodNode.instructions.toArray();
                                    for (int i = codeStart; i < codeEnd; i++) {
                                        methodNode.instructions.remove(nodes[i]);
                                    }
                                });

                        {
                            if (!switchNode.dflt.equals(originalCodeBlockStart) && methodNode.instructions.indexOf(switchNode.dflt) < methodNode.instructions.indexOf(switchNode)) {
                                LabelNode codeBlockStart = switchNode.dflt;
                                AbstractInsnNode codeBlockEnd = codeBlockStart;

                                {
                                    while (codeBlockEnd.equals(codeBlockStart) || !(codeBlockEnd instanceof LabelNode)) {
                                        if (codeBlockEnd.getNext() == null) {
                                            break;
                                        }

                                        codeBlockEnd = codeBlockEnd.getNext();
                                    }
                                }

                                boolean canExtract = codeBlockEnd instanceof LabelNode || (codeBlockEnd.getOpcode() == ATHROW && codeBlockEnd.getPrevious().getOpcode() == ACONST_NULL);
                                if (!canExtract)
                                    return;

                                int codeStart = methodNode.instructions.indexOf(codeBlockStart) + 1;
                                int codeEnd = methodNode.instructions.indexOf(codeBlockEnd);

                                if (codeBlockEnd instanceof LabelNode)
                                    codeEnd--;

                                AbstractInsnNode[] nodes = methodNode.instructions.toArray();
                                for (int i = codeStart; i < codeEnd; i++) {
                                    methodNode.instructions.remove(nodes[i]);
                                }
                            }
                        }

                        methodNode.instructions.remove(node.getPrevious());
                        methodNode.instructions.remove(node);
                    }
                });
    }

    private void fourthPhase(Deobfuscator deobfuscator) {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    Arrays.stream(methodNode.instructions.toArray())
                            .filter(node -> node.getOpcode() == ACONST_NULL)
                            .filter(node -> check(node.getNext(), GETSTATIC))
                            .filter(node -> check(node.getNext().getNext(), JumpInsnNode.class))
                            .forEach(node -> {
                                methodNode.instructions.remove(node.getNext().getNext());
                                methodNode.instructions.remove(node.getNext());
                            });
                });
    }

    private void fifthPhase(Deobfuscator deobfuscator) {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    Arrays.stream(methodNode.instructions.toArray())
                            .filter(node -> node.getOpcode() == DUP)
                            .filter(node -> check(node.getNext(), IFNULL))
                            .filter(node -> check(node.getNext().getNext(), ATHROW))
                            .forEach(node -> {
                                methodNode.instructions.remove(node.getNext().getNext());
                                methodNode.instructions.remove(node.getNext());
                                methodNode.instructions.remove(node);
                            });
                });
    }

    private void clean(Deobfuscator deobfuscator) {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    boolean modified;
                    do {
                        modified = false;

                        for (AbstractInsnNode node : methodNode.instructions.toArray()) {
                            if (node.getOpcode() == GOTO
                                    && node.getNext() != null && node.getNext().getOpcode() == ATHROW
                                    && node.getNext().getNext() != null && ((JumpInsnNode) node).label.equals(node.getNext().getNext())) {
                                methodNode.instructions.remove(node.getNext());
                                methodNode.instructions.remove(node);
                                modified = true;
                            } else if (node.getOpcode() == ACONST_NULL) {
                                if (check(node.getNext(), LabelNode.class) && check(node.getNext().getNext(), POP)) {
                                    methodNode.instructions.remove(node.getNext().getNext());
                                    methodNode.instructions.remove(node);
                                    modified = true;
                                } else if (check(node.getNext(), POP)) {
                                    methodNode.instructions.remove(node.getNext());
                                    methodNode.instructions.remove(node);
                                    modified = true;
                                }
                            }
                        }
                    } while (modified);
                });
    }
}
