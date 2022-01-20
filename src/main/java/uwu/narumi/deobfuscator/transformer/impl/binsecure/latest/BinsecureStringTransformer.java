package uwu.narumi.deobfuscator.transformer.impl.binsecure.latest;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.exception.TransformerException;
import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.helper.ClassHelper;
import uwu.narumi.deobfuscator.sandbox.SandBox;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BinsecureStringTransformer extends Transformer {

    //private final String decryptClassName;
    //private final String mapClassName;
    private final boolean useStackAnalyzer;

    //public BinsecureStringTransformer() {
    //    this("c", "0", false);
    // }

    public BinsecureStringTransformer(/*String decryptClassName, String mapClassName, */boolean useStackAnalyzer) {
        // this.decryptClassName = decryptClassName;
        // this.mapClassName = mapClassName;
        this.useStackAnalyzer = useStackAnalyzer;
    }

    /*
        Speed as fuck boiiii
     */
    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        //ClassNode mapClass = deobfuscator.getOriginalClasses().get(mapClassName);
        //ClassNode decryptClass = ClassHelper.copy(deobfuscator.getOriginalClasses().get(decryptClassName));
        ClassNode decryptClass = ClassHelper.copy(searchForKeyClass(deobfuscator));
        if (/*mapClass == null ||*/ decryptClass == null)
            return;

        int key = getKey(decryptClass);

        decryptClass.fields.removeIf(fieldNode -> !fieldNode.desc.startsWith("["));
        decryptClass.methods.removeIf(methodNode -> !methodNode.name.startsWith("<"));
        findClInit(decryptClass).ifPresent(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                .filter(node -> node.getOpcode() == NEW)
                .filter(node -> node.getNext().getOpcode() == DUP)
                .filter(node -> node.getNext().getNext().getOpcode() == INVOKESPECIAL)
                .filter(node -> node.getNext().getNext().getNext().getOpcode() == PUTSTATIC)
                .findFirst()
                .ifPresent(node -> {
                    methodNode.instructions.remove(node.getNext().getNext().getNext());
                    methodNode.instructions.remove(node.getNext().getNext());
                    methodNode.instructions.remove(node.getNext());
                    methodNode.instructions.remove(node);
                }));

        SandBox sandBox = SandBox.getInstance();
        sandBox.put(/*mapClass,*/ decryptClass);

        String fieldName = decryptClass.fields.stream().filter(node -> node.desc.equals("[I")).map(node -> node.name).findFirst().orElse("aiooi1iojionlknzjsdnfdas");
        int[] keys = (int[]) sandBox.get(decryptClass).get(fieldName, null);

        if (keys == null)
            throw new TransformerException();

        deobfuscator.classes().forEach(classNode -> classNode.methods.forEach(methodNode -> {
            if (Arrays.stream(methodNode.instructions.toArray()).noneMatch(ASMHelper::isString))
                return;

            if (useStackAnalyzer) {
                decryptUsingAnalyzer(classNode, methodNode, decryptClass, key, keys);
            } else {
                decryptNormally(classNode, methodNode, decryptClass, key, keys);
            }
        }));
    }

    private void decryptNormally(ClassNode classNode, MethodNode methodNode, ClassNode decryptClass, int key, int[] keys) {
        Arrays.stream(methodNode.instructions.toArray())
                .filter(ASMHelper::isString)
                .filter(node -> node.getNext() instanceof MethodInsnNode || (node.getNext() instanceof LabelNode && node.getNext().getNext() instanceof MethodInsnNode))
                .map(node -> (MethodInsnNode) (node.getNext() instanceof LabelNode ? node.getNext().getNext() : node.getNext()))
                .filter(node -> node.owner.equals(decryptClass.name))
                .forEach(node -> {
                    String string = getString(node.getPrevious() instanceof LabelNode ? node.getPrevious().getPrevious() : node.getPrevious());
                    int classHash = classNode.name.replace('/', '.').hashCode();
                    int methodHash = methodNode.name.replace('/', '.').hashCode();

                    methodNode.instructions.remove(node.getPrevious() instanceof LabelNode ? node.getPrevious().getPrevious() : node.getPrevious());
                    methodNode.instructions.set(node, new LdcInsnNode(decrypt(string, key, classHash, methodHash, keys)));
                });
    }

    /*
        I think this method is better but its slower but it can deobfuscate fucked shit like

        LDC "encrypted string"
        LABELNODE
        INVOKESTATIC

        etc
     */
    private void decryptUsingAnalyzer(ClassNode classNode, MethodNode methodNode, ClassNode decryptClass, int key, int[] keys) {
        Map<AbstractInsnNode, Frame<SourceValue>> frames = analyzeSource(classNode, methodNode);
        if (frames == null) {
            decryptNormally(classNode, methodNode, decryptClass, key, keys);
        } else {
            Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node instanceof MethodInsnNode)
                    .map(MethodInsnNode.class::cast)
                    .filter(node -> node.owner.equals(decryptClass.name))
                    .filter(frames::containsKey)
                    .forEach(node -> {
                        Frame<SourceValue> frame = frames.get(node);
                        SourceValue value = frame.getStack(frame.getStackSize() - 1);
                        if (value == null || value.insns == null || value.insns.isEmpty())
                            return;

                        AbstractInsnNode stackInsn = value.insns.iterator().next();
                        if (!isString(stackInsn))
                            return;

                        String string = getString(stackInsn);
                        int classHash = classNode.name.replace('/', '.').hashCode();
                        int methodHash = methodNode.name.replace('/', '.').hashCode();

                        methodNode.instructions.remove(stackInsn);
                        methodNode.instructions.set(node, new LdcInsnNode(decrypt(string, key, classHash, methodHash, keys)));
                    });
        }
    }

    private int getKey(ClassNode classNode) {
        AtomicInteger key = new AtomicInteger();
        classNode.methods.stream()
                .filter(methodNode -> methodNode.desc.equals("(Ljava/lang/String;I)Ljava/lang/String;"))
                .findFirst().flatMap(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                        .filter(ASMHelper::isInteger)
                        .filter(node -> check(node.getNext(), ISTORE))
                        .filter(node -> check(node.getNext().getNext(), ISTORE))
                        .filter(node -> check(node.getNext().getNext().getNext(), ICONST_M1))
                        .filter(node -> check(node.getNext().getNext().getNext().getNext(), ACONST_NULL))
                        .findFirst()).ifPresent(node -> key.set(ASMHelper.getInteger(node)));

        return key.get();
    }

    /*
        Fuck kotlin
     */
    private String decrypt(String string, int key, int classHash, int methodHash, int[] keys) {
        char[] chars = string.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            int hash = -1;
            switch (i % 5) {
                case 0:
                    hash = 4 + classHash;
                    break;
                case 1:
                    hash = key;
                    break;
                case 2:
                    hash = classHash;
                    break;
                case 3:
                    hash = methodHash;
                    break;
                case 4:
                    hash = methodHash + classHash;
                    break;
                case 5:
                    hash = i + methodHash;
                    break;
            }

            chars[i] ^= (hash) ^ keys[i % keys.length];
        }

        return new String(chars);
    }

    private ClassNode searchForKeyClass(Deobfuscator deobfuscator) {
        return deobfuscator.classes().stream()
                .filter(classNode -> classNode.fields.stream().anyMatch(fieldNode -> fieldNode.desc.endsWith("[[[[[[[I")))
                .findFirst()
                .orElseThrow();
    }
}