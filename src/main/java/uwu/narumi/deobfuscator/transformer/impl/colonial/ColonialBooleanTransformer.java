package uwu.narumi.deobfuscator.transformer.impl.colonial;

import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

/**
 * @author Szymon on 02.02.2022
 * @project Deobfuscator
 */
public class ColonialBooleanTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes()
                .forEach(classNode ->
                        classNode.methods.forEach(methodNode ->
                                Arrays.stream(methodNode.instructions.toArray())
                                        .filter(insnNode -> insnNode instanceof InsnNode)
                                        .forEach(insnNode -> {
                                            //lel
                                            switch (insnNode.getOpcode()) {
                                                case IDIV: {
                                                    if (insnNode.getPrevious() instanceof LdcInsnNode
                                                            && insnNode.getPrevious().getPrevious() instanceof InsnNode
                                                            && insnNode.getPrevious().getPrevious().getPrevious() instanceof LdcInsnNode
                                                            && (Integer) ((LdcInsnNode) insnNode.getPrevious().getPrevious().getPrevious()).cst <= 7) {
                                                        methodNode.instructions.remove(insnNode.getPrevious().getPrevious().getPrevious());
                                                        methodNode.instructions.remove(insnNode.getPrevious().getPrevious());
                                                        methodNode.instructions.remove(insnNode.getPrevious());
                                                        methodNode.instructions.remove(insnNode);
                                                    }
                                                    break;
                                                }
                                                case IREM: {
                                                    if (insnNode.getPrevious() instanceof LdcInsnNode
                                                            //should work
                                                            && ((Integer) ((LdcInsnNode) insnNode.getPrevious()).cst - 1) <= 32767) {
                                                        methodNode.instructions.remove(insnNode.getPrevious());
                                                        methodNode.instructions.remove(insnNode);
                                                    }
                                                    break;
                                                }
                                                case IXOR: {
                                                    if (insnNode.getPrevious() instanceof FieldInsnNode
                                                            && insnNode.getPrevious().getOpcode() == GETSTATIC) {
                                                        methodNode.instructions.remove(insnNode.getPrevious());
                                                        methodNode.instructions.remove(insnNode);
                                                    }
                                                    break;
                                                }
                                                case IADD: {
                                                    if (insnNode.getPrevious() instanceof LdcInsnNode
                                                            //should work
                                                            && ((Integer) ((LdcInsnNode) insnNode.getPrevious()).cst / 2 - 1) <= 32767) {
                                                        methodNode.instructions.remove(insnNode.getPrevious());
                                                        methodNode.instructions.remove(insnNode);
                                                    }
                                                    break;
                                                }
                                                default:
                                                    break;
                                            }
                                        })));
    }
}
