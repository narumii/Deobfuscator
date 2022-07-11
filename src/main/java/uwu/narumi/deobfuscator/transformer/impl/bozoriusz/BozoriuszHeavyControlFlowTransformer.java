package uwu.narumi.deobfuscator.transformer.impl.bozoriusz;

import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class BozoriuszHeavyControlFlowTransformer extends Transformer {

    /*
        I just gave up XD dont want waste time to search what i fucked up
     */
    private final boolean fuckingASMDogShit;

    public BozoriuszHeavyControlFlowTransformer(boolean fuckingASMDogShit) {
        this.fuckingASMDogShit = fuckingASMDogShit;
    }

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().forEach(classNode -> {
            classNode.fields.removeIf(field -> (field.name.equals("Ꮹ") || field.name.matches("[Il]{50,}")) && field.desc.equals("J"));
            classNode.methods.forEach(methodNode -> {
                if (!fuckingASMDogShit) {
                    Arrays.stream(methodNode.instructions.toArray())
                            .filter(node -> node instanceof FieldInsnNode)
                            .map(FieldInsnNode.class::cast)
                            .filter(node -> node.name.equals("Ꮹ") || node.name.matches("[Il]{50,}"))
                            .filter(node -> node.desc.equals("J"))
                            .filter(node -> node.owner.contains(classNode.name))
                            .filter(node -> node.getNext().getOpcode() == L2I)
                            .filter(node -> node.getNext().getNext() instanceof LookupSwitchInsnNode)
                            .forEach(node -> {
                                LookupSwitchInsnNode switchNode = (LookupSwitchInsnNode) node.getNext().getNext();
                                LabelNode labelNode = switchNode.labels.stream().filter(label -> !label.equals(switchNode.dflt)).findFirst().orElseThrow();
                                if (labelNode.getNext().getOpcode() == GOTO) {
                                    getInstructionsBetween(
                                            node,
                                            ((JumpInsnNode) labelNode.getNext()).label,
                                            true,
                                            true
                                    ).forEach(methodNode.instructions::remove);
                                }
                            });

                    Arrays.stream(methodNode.instructions.toArray())
                            .filter(node -> node instanceof FieldInsnNode)
                            .map(FieldInsnNode.class::cast)
                            .filter(node -> node.name.equals("Ꮹ") || node.name.matches("[Il]{50,}"))
                            .filter(node -> node.desc.equals("J"))
                            .filter(node -> node.owner.contains(classNode.name))
                            .filter(node -> node.getNext() instanceof LabelNode)
                            .filter(node -> isNumber(node.getNext().getNext()))
                            .filter(node -> node.getNext().getNext().getNext().getOpcode() == GOTO)
                            .forEach(node -> {
                                JumpInsnNode gotoSwitch = (JumpInsnNode) node.getNext().getNext().getNext();
                                LookupSwitchInsnNode switchNode = (LookupSwitchInsnNode) gotoSwitch.label.getNext().getNext().getNext();
                                LabelNode labelNode = switchNode.labels.stream().filter(label -> !label.equals(switchNode.dflt)).findFirst().orElseThrow();

                                JumpInsnNode gotoSecondPart = ((JumpInsnNode) labelNode.getNext().getNext().getNext());
                                LabelNode secondPartLabel = gotoSecondPart.label;

                                int index = methodNode.instructions.indexOf(secondPartLabel);
                                if (secondPartLabel.getNext().getOpcode() == LXOR) {
                                    AbstractInsnNode end = methodNode.instructions.get(index + 11);
                                    if (end instanceof LabelNode) {
                                        getInstructionsBetween(
                                                node,
                                                end,
                                                true,
                                                true
                                        ).forEach(methodNode.instructions::remove);
                                    }
                                }
                            });

                    Arrays.stream(methodNode.instructions.toArray())
                            .filter(node -> node instanceof FieldInsnNode)
                            .map(FieldInsnNode.class::cast)
                            .filter(node -> node.name.equals("Ꮹ") || node.name.matches("[Il]{50,}"))
                            .filter(node -> node.desc.equals("J"))
                            .filter(node -> node.owner.contains(classNode.name))
                            .filter(node -> isNumber(node.getNext()))
                            .filter(node -> node.getNext().getNext().getOpcode() == GOTO)
                            .forEach(node -> {
                                JumpInsnNode gotoSwitch = (JumpInsnNode) node.getNext().getNext();
                                LookupSwitchInsnNode switchNode = (LookupSwitchInsnNode) gotoSwitch.label.getNext().getNext().getNext();
                                LabelNode labelNode = switchNode.labels.stream().filter(label -> !label.equals(switchNode.dflt)).findFirst().orElseThrow();

                                JumpInsnNode gotoSecondPart = ((JumpInsnNode) labelNode.getNext().getNext().getNext());
                                LabelNode secondPartLabel = gotoSecondPart.label;

                                int index = methodNode.instructions.indexOf(secondPartLabel);
                                if (secondPartLabel.getNext().getOpcode() == LCMP) {
                                    AbstractInsnNode end = methodNode.instructions.get(index + 4);
                                    if (end.getOpcode() == IFEQ) {
                                        end = methodNode.instructions.get(index + 7);
                                    }

                                    getInstructionsBetween(
                                            node,
                                            end,
                                            true,
                                            true
                                    ).forEach(methodNode.instructions::remove);
                                } else if (secondPartLabel.getNext().getOpcode() == LAND) {
                                    AbstractInsnNode end = methodNode.instructions.get(index + 4);
                                    AbstractInsnNode afterStart = methodNode.instructions.get(index + 6);
                                    AbstractInsnNode afterEnd = methodNode.instructions.get(index + 12);

                                    getInstructionsBetween(
                                            node,
                                            end,
                                            true,
                                            true
                                    ).forEach(methodNode.instructions::remove);

                                    getInstructionsBetween(
                                            afterStart,
                                            afterEnd,
                                            true,
                                            true
                                    ).forEach(methodNode.instructions::remove);
                                }
                            });
                } else {
                    Arrays.stream(methodNode.instructions.toArray())
                            .filter(this::is)
                            .filter(node -> node.getNext().getOpcode() == GOTO)
                            .filter(node -> node.getPrevious().getOpcode() == POP)
                            .forEach(node -> {
                                getInstructionsBetween(
                                        node.getNext(),
                                        ((JumpInsnNode) node.getNext()).label,
                                        true,
                                        true
                                ).stream().filter(xd -> !(xd instanceof LabelNode)).forEach(methodNode.instructions::remove);

                                int index = methodNode.instructions.indexOf(node);
                                AbstractInsnNode start = methodNode.instructions.get(index - 13);

                                getInstructionsBetween(
                                        start,
                                        node.getPrevious(),
                                        true,
                                        true
                                ).stream().filter(xd -> !(xd instanceof LabelNode)).forEach(methodNode.instructions::remove);
                            });

                    if (methodNode.instructions.size() > 0) {
                        AbstractInsnNode start = methodNode.instructions.getFirst() != null ? methodNode.instructions.getFirst() : methodNode.instructions.get(0);
                        if (start.getOpcode() == ACONST_NULL && start.getNext().getOpcode() == ASTORE) {
                            methodNode.instructions.remove(start.getNext());
                            methodNode.instructions.remove(start);

                            start = methodNode.instructions.getFirst();
                        }

                        if (start.getOpcode() == ICONST_1 && start.getNext().getOpcode() == GOTO) {
                            methodNode.instructions.remove(start.getNext().getNext().getNext().getNext().getNext().getNext());
                            methodNode.instructions.remove(start.getNext().getNext().getNext().getNext().getNext());
                            methodNode.instructions.remove(start.getNext().getNext().getNext().getNext());
                            methodNode.instructions.remove(start.getNext().getNext().getNext());
                            methodNode.instructions.remove(start.getNext().getNext());
                            methodNode.instructions.remove(start.getNext());
                            methodNode.instructions.remove(start);
                        }
                    }
                }
            });
        });
    }

    private boolean is(AbstractInsnNode node) {
        return node.getOpcode() == NEW || node instanceof FieldInsnNode || node instanceof MethodInsnNode || node instanceof InvokeDynamicInsnNode;
    }
}
