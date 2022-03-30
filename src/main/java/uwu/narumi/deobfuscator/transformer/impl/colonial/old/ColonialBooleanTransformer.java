package uwu.narumi.deobfuscator.transformer.impl.colonial.old;

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
                        classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                                .filter(insnNode -> insnNode instanceof InsnNode)
                                .filter(insnNode -> insnNode.getPrevious() instanceof LdcInsnNode)
                                .forEach(insnNode -> {
                                    switch (insnNode.getOpcode()) {
                                        case IDIV: {
                                            if (getInteger(insnNode.getPrevious().getPrevious().getPrevious()) <= 7) {
                                                methodNode.instructions.remove(insnNode.getPrevious().getPrevious().getPrevious());
                                                methodNode.instructions.remove(insnNode.getPrevious().getPrevious());
                                                methodNode.instructions.remove(insnNode.getPrevious());
                                                methodNode.instructions.remove(insnNode);
                                            }
                                            break;
                                        }
                                        case IREM: {
                                            if ((getInteger(insnNode.getPrevious()) - 1) <= 32767) {
                                                methodNode.instructions.remove(insnNode.getPrevious());
                                                methodNode.instructions.remove(insnNode);
                                            }
                                            break;
                                        }
                                        case IXOR: {
                                            if (insnNode.getPrevious().getOpcode() == GETSTATIC) {
                                                methodNode.instructions.remove(insnNode.getPrevious());
                                                methodNode.instructions.remove(insnNode);
                                            }
                                            break;
                                        }
                                        case IADD: {
                                            if ((getInteger(insnNode.getPrevious()) / 2 - 1) <= 32767) {
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
