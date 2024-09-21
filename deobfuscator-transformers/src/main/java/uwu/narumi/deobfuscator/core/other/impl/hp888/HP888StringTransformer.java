package uwu.narumi.deobfuscator.core.other.impl.hp888;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class HP888StringTransformer extends Transformer {

    @Override
    protected void transform(ClassWrapper scope, Context context) throws Exception {
        context.classes().stream().map(ClassWrapper::classNode).forEach(classNode -> {
            List<MethodNode> toRemove = new ArrayList<>();
            classNode.methods.forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node.getOpcode() == INVOKESTATIC)
                    .filter(node -> node.getPrevious() != null && node.getPrevious().isString())
                    .map(MethodInsnNode.class::cast)
                    .filter(node -> node.owner.equals(classNode.name))
                    .filter(node -> node.desc.equals("(Ljava/lang/String;)Ljava/lang/String;"))
                    .forEach(node -> findMethod(classNode, method -> method.name.equals(node.name) && method.desc.equals(node.desc)).ifPresent(method -> {
                        String string = node.getPrevious().asString();
                        int constantPoolSize = context.getClasses().get(getClassToPool(method).orElseThrow().getInternalName()).getConstantPool().getSize();
                        String class0 = classNode.name;
                        String class1 = classNode.name;
                        methodNode.instructions.remove(node.getPrevious());
                        methodNode.instructions.set(node, new LdcInsnNode(decrypt(string, constantPoolSize, class0.hashCode(), class1.hashCode())));
                        markChange();
                        toRemove.add(method);
                    })));
            classNode.methods.removeAll(toRemove);
            toRemove.clear();
        });
    }

    private Optional<Type> getClassToPool(MethodNode methodNode) {
        return Arrays.stream(methodNode.instructions.toArray())
                .filter(node -> node.getOpcode() == INVOKESTATIC)
                .map(AbstractInsnNode::next)
                .filter(next -> next.getOpcode() == LDC)
                .filter(next -> next.next().getOpcode() == INVOKEINTERFACE)
                .filter(abstractInsnNode -> abstractInsnNode instanceof LdcInsnNode)
                .map(LdcInsnNode.class::cast)
                .map(ldcInsnNode -> ldcInsnNode.cst)
                .map(Type.class::cast)
                .findFirst();
    }

    private String decrypt(String string, int constantPoolSize, int className0HashCode, int className1HashCode) {
        char[] lllIlIIIllIllIIIllIlIllIl = string.toCharArray();
        int llIllIIIlllllIllIIlIIlIIl = 0;
        for (char IlIIlIlllIIllIIIllIIIIIll : lllIlIIIllIllIIIllIlIllIl) {
            IlIIlIlllIIllIIIllIIIIIll = (char)(IlIIlIlllIIllIIIllIIIIIll ^ constantPoolSize);
            IlIIlIlllIIllIIIllIIIIIll = (char)(IlIIlIlllIIllIIIllIIIIIll ^ className0HashCode);
            lllIlIIIllIllIIIllIlIllIl[llIllIIIlllllIllIIlIIlIIl] = (char)(IlIIlIlllIIllIIIllIIIIIll ^ className1HashCode);
            ++llIllIIIlllllIllIIlIIlIIl;
        }
        return new String(lllIlIIIllIllIIIllIlIllIl);
    }
}
