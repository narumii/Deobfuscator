package uwu.narumi.deobfuscator.transformer.impl.qprotect.latest;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.TransformerException;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/*
    TODO: A lot of cleaning and optimization
    TODO: Fields removing and clinit cleaning
 */
public class qProtectStringTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        Map<ClassNode, MethodNode> toRemove = new HashMap<>();
        deobfuscator.classes().forEach(classNode -> {
            AtomicReference<ClassInformation> classInformation = new AtomicReference<>();
            classNode.methods
                    .forEach(methodNode -> {
                        Map<AbstractInsnNode, Frame<SourceValue>> frames = analyzeSource(classNode, methodNode);

                        Arrays.stream(methodNode.instructions.toArray())
                                .filter(node -> node instanceof MethodInsnNode)
                                .filter(node -> node.getOpcode() == INVOKESTATIC)
                                .map(MethodInsnNode.class::cast)
                                .filter(node -> node.desc.equals("(II)Ljava/lang/String;"))
                                .filter(node -> deobfuscator.getClasses().containsKey(node.owner))
                                .forEach(node -> {
                                    if (classInformation.get() == null) {
                                        ClassNode owner = deobfuscator.getClasses().get(node.owner);
                                        findMethod(owner, method -> method.name.equals(node.name) && method.desc.equals(node.desc))
                                                .ifPresent(decrypt -> {
                                                    classInformation.set(new ClassInformation(getKey(decrypt), getKeys(decrypt), getStrings(deobfuscator, decrypt)));
                                                    toRemove.put(classNode, decrypt);
                                                });
                                    }

                                    try {
                                        Frame<SourceValue> frame = frames.get(node);
                                        SourceValue first = frame.getStack(frame.getStackSize() - 2); //first
                                        SourceValue second = frame.getStack(frame.getStackSize() - 1); //second

                                        AbstractInsnNode a = first.insns.iterator().next();
                                        AbstractInsnNode b = second.insns.iterator().next();

                                        methodNode.instructions.remove(a);
                                        methodNode.instructions.remove(b);
                                        methodNode.instructions.set(node, new LdcInsnNode(decrypt(getInteger(a), getInteger(b), classInformation.get())));
                                    } catch (Exception ignored) {
                                        //ignored this xd
                                    }
                                });
                    });
        });

        toRemove.forEach(((classNode, methodNode) -> classNode.methods.remove(methodNode)));
    }

    private Map<Integer, String> getStrings(Deobfuscator deobfuscator, MethodNode decrypt) {
        Map<Integer, String> strings = new HashMap<>();

        FieldInsnNode fieldInsnNode = Arrays.stream(decrypt.instructions.toArray())
                .filter(node -> node instanceof FieldInsnNode)
                .map(FieldInsnNode.class::cast)
                .filter(node -> node.getOpcode() == GETSTATIC)
                .filter(node -> node.desc.equals("[Ljava/lang/String;"))
                .filter(node -> node.getPrevious().getOpcode() == IFNONNULL)
                .findFirst().orElseThrow();


        ClassNode classNode = deobfuscator.getClasses().get(fieldInsnNode.owner);
        MethodNode methodNode = findClInit(classNode).orElseThrow();

        AbstractInsnNode start = null;
        AbstractInsnNode end = null;

        for (AbstractInsnNode node : methodNode.instructions.toArray()) {
            if (node instanceof TypeInsnNode && node.getOpcode() == ANEWARRAY && ((TypeInsnNode) node).desc.equals("java/lang/String")) {
                AbstractInsnNode current = node;
                while (!(current instanceof FieldInsnNode && ((FieldInsnNode) current).name.equals(fieldInsnNode.name) && current.getOpcode() == PUTSTATIC)) {
                    current = current.getNext();
                    end = current;
                }
                start = node;
            }
        }

        if (start == null)
            throw new TransformerException();

        List<AbstractInsnNode> nodes = new ArrayList<>();
        for (int i = methodNode.instructions.indexOf(start); i < methodNode.instructions.indexOf(end); i++) {
            AbstractInsnNode insn = methodNode.instructions.get(i);
            if (insn.getOpcode() == AASTORE)
                nodes.add(insn);
        }

        if (nodes.isEmpty())
            throw new TransformerException();

        Map<AbstractInsnNode, Frame<SourceValue>> frames = analyzeSource(classNode, methodNode);
        if (frames == null)
            throw new TransformerException();

        nodes.forEach(node -> {
            try {
                Frame<SourceValue> frame = frames.get(node);
                SourceValue intStack = frame.getStack(frame.getStackSize() - 2);
                SourceValue stringStack = frame.getStack(frame.getStackSize() - 1);

                AbstractInsnNode a = intStack.insns.iterator().next();
                AbstractInsnNode b = stringStack.insns.iterator().next();

                strings.put(getInteger(a), getString(b));
            } catch (Exception ignored) {
                //ignore this xd
            }
        });

        return strings;
    }

    private int getKey(MethodNode methodNode) {
        return Arrays.stream(methodNode.instructions.toArray())
                .filter(ASMHelper::isInteger)
                .filter(node -> node.getNext().getOpcode() == IXOR)
                .filter(node -> node.getPrevious().getOpcode() == ILOAD)
                .map(ASMHelper::getInteger)
                .findFirst()
                .orElse(-1);
    }

    private Map<Integer, Integer> getKeys(MethodNode methodNode) {
        Map<Integer, Integer> keys = new HashMap<>();
        Arrays.stream(methodNode.instructions.toArray()).filter(node -> node instanceof TableSwitchInsnNode)
                .map(TableSwitchInsnNode.class::cast)
                .findFirst()
                .ifPresent(table -> {
                    for (int i = 0; i < table.labels.size(); i++) {
                        keys.put(i, getInteger(table.labels.get(i).getNext()));
                    }

                    keys.put(255, getInteger(table.dflt.getNext()));
                });

        return keys;
    }

    private String decrypt(int first, int second, ClassInformation classInformation) {
        int position = (first ^ classInformation.key) & '\uffff';
        char[] chars = classInformation.strings.get(position).toCharArray();
        int switchKey = classInformation.keys.get(chars[0] & 255);

        int a = ((short) second & 255) - switchKey;
        if (a < 0) {
            a += 256;
        }

        int b = (((short) second & '\uffff') >>> 8) - switchKey;
        if (b < 0) {
            b += 256;
        }

        for (int i = 0; i < chars.length; ++i) {
            int type = i % 2;
            char newChars = chars[i];
            if (type == 0) {
                chars[i] = (char) (newChars ^ a);
                a = ((a >>> 3 | a << 5) ^ chars[i]) & 255;
            } else {
                chars[i] = (char) (newChars ^ b);
                b = ((b >>> 3 | b << 5) ^ chars[i]) & 255;
            }
        }


        return new String(chars);
    }

    private class ClassInformation {
        final int key;
        final Map<Integer, Integer> keys;
        final Map<Integer, String> strings;

        public ClassInformation(int key, Map<Integer, Integer> keys, Map<Integer, String> strings) {
            this.key = key;
            this.keys = keys;
            this.strings = strings;
        }
    }
}