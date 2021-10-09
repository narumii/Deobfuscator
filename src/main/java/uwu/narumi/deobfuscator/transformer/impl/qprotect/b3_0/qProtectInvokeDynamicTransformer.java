package uwu.narumi.deobfuscator.transformer.impl.qprotect.b3_0;

import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

import java.util.Arrays;

/*
    This transformer works on versions: 3.0-b1 and b31
 */
public class qProtectInvokeDynamicTransformer extends Transformer {

    @Override
    public void transform(Deobfuscator deobfuscator) throws Exception {
        deobfuscator.classes().stream()
                .flatMap(classNode -> classNode.methods.stream())
                .forEach(methodNode -> Arrays.stream(methodNode.instructions.toArray())
                        .filter(node -> node instanceof InvokeDynamicInsnNode)
                        .map(InvokeDynamicInsnNode.class::cast)
                        .filter(node -> node.bsm.getDesc().equals("(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
                        .filter(node -> node.bsmArgs.length == 10)
                        .forEach(node -> methodNode.instructions.set(node, decrypt(node.bsmArgs))));
    }

    private MethodInsnNode decrypt(Object[] args) {
        char[] var14 = args[1].toString().toCharArray();
        char[] var15 = new char[var14.length];
        char[] var16 = args[4].toString().toCharArray();
        char[] var17 = args[5].toString().toCharArray();

        for (int var18 = 0; var18 < var14.length; ++var18) {
            var15[var18] = (char) (var14[var18] ^ var16[var18 % var16.length]);
        }

        char[] var30 = new char[var15.length];

        for (int var19 = 0; var19 < var14.length; ++var19) {
            var30[var19] = (char) (var15[var19] ^ var17[var19 % var17.length]);
        }

        char[] var31 = args[2].toString().toCharArray();
        char[] var20 = new char[var31.length];
        char[] var21 = args[6].toString().toCharArray();
        char[] var22 = args[7].toString().toCharArray();

        for (int var23 = 0; var23 < var31.length; ++var23) {
            var20[var23] = (char) (var31[var23] ^ var21[var23 % var21.length]);
        }

        char[] var32 = new char[var20.length];

        for (int var24 = 0; var24 < var31.length; ++var24) {
            var32[var24] = (char) (var20[var24] ^ var22[var24 % var22.length]);
        }

        char[] var33 = args[3].toString().toCharArray();
        char[] var25 = new char[var33.length];
        char[] var26 = args[8].toString().toCharArray();
        char[] var27 = args[9].toString().toCharArray();

        for (int var28 = 0; var28 < var33.length; ++var28) {
            var25[var28] = (char) (var33[var28] ^ var26[var28 % var26.length]);
        }

        char[] var34 = new char[var25.length];

        int type;
        for (type = 0; type < var33.length; ++type) {
            var34[type] = (char) (var25[type] ^ var27[type % var27.length]);
        }
        type = (Integer) args[0];

        String className = new String(var30).replace('.', '/');
        String methodName = new String(var32);
        String desc = new String(var34);

        return new MethodInsnNode(type == 0 ? INVOKESTATIC : INVOKEVIRTUAL, className, methodName, desc, false);
    }
}
