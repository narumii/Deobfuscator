package uwu.narumi.deobfuscator.transformer.impl.bozoriusz;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

public class BozoriuszStringTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> {
                    Arrays.stream(methodNode.instructions.toArray())
                            .filter(node -> node.getOpcode() == NEW)
                            .map(TypeInsnNode.class::cast)
                            .filter(node -> node.desc.equals("java/lang/String"))
                            .filter(node -> node.getNext().getOpcode() == DUP)
                            .filter(node -> node.getNext().getNext().getOpcode() == ALOAD)
                            .filter(node -> node.getNext().getNext().getNext().getOpcode() == INVOKESPECIAL)
                            .forEach(node -> {
                                AbstractInsnNode current = node;

                                int endIndex = methodNode.instructions.indexOf(node);
                                int startIndex = -1;
                                int storeIndex = ((VarInsnNode) node.getNext().getNext()).var;

                                while (current.getPrevious() != null && !((current = current.getPrevious()).getOpcode() == ASTORE && ((VarInsnNode) current).var == storeIndex)) {
                                }

                                startIndex = methodNode.instructions.indexOf(current);
                                if (startIndex == -1 || !isInteger(current.getPrevious().getPrevious()))
                                    return;

                                byte[] bytes = new byte[getInteger(current.getPrevious().getPrevious())];
                                for (int i = startIndex; i < endIndex; i++) {
                                    AbstractInsnNode insn = methodNode.instructions.get(i);
                                    if (isInteger(insn) && isInteger(insn.getNext()) && insn.getNext().getNext().getOpcode() == BASTORE) {
                                        bytes[getInteger(insn)] = (byte) getInteger(insn.getNext());
                                    }
                                }

                                AbstractInsnNode insertBefore = node.getNext().getNext().getNext().getNext();
                                getInstructionsBetween(current.getPrevious().getPrevious(), node.getNext().getNext().getNext(), true, true)
                                        .forEach(methodNode.instructions::remove);

                                methodNode.instructions.insertBefore(insertBefore, new LdcInsnNode(new String(bytes)));
                            });

                });
    }
}
