package uwu.narumi.deobfuscator.core.other.impl.zkm.helper;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;

import java.util.Map;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

/**
 * Better detection of Zelix string decryption calls.
 * @author d-o-g
 */
public class ZelixStringDecryptionMatch extends Match {

    @Override
    protected boolean test(MatchContext ctx) {
        AbstractInsnNode insn = ctx.insn();
        if (!(insn instanceof MethodInsnNode call))
            return false;

        if (!"(II)Ljava/lang/String;".equals(call.desc)) {
            return false;
        }

        MethodContext mc = ctx.insnContext().methodContext();

        if (!call.owner.equals(mc.classWrapper().name()))
            return false;

        if (call.getOpcode() != INVOKESTATIC) {
            return false;
        }

        Map<AbstractInsnNode, Frame<OriginalSourceValue>> frms = mc.frames();
        Frame<OriginalSourceValue> f = frms.get(insn);
        if (f == null) {
            return false;
        }

        OriginalSourceValue arg2 = f.getStack(f.getStackSize() - 1);
        OriginalSourceValue arg1 = f.getStack(f.getStackSize() - 2);

        AbstractInsnNode p1 = singleConstProducer(arg1);
        AbstractInsnNode p2 = singleConstProducer(arg2);
        if (p1 == null || p2 == null) {
            return false;
        }

        ctx.captures().put("key1", MatchContext.of(mc.at(p1)));
        ctx.captures().put("key2", MatchContext.of(mc.at(p2)));
        ctx.captures().put("method-node", ctx);

        ctx.collectedInsns().add(p1);
        ctx.collectedInsns().add(p2);
        ctx.collectedInsns().add(insn);
        return true;
    }

    private static AbstractInsnNode singleConstProducer(OriginalSourceValue sv) {
        if (sv == null || sv.insns == null || sv.insns.isEmpty()) {
            return null;
        }

        AbstractInsnNode only = null;
        for (AbstractInsnNode p : sv.insns) {
            if (!p.isInteger()) {
                return null;
            }

            if (only == null) {
                only = p;
            } else if (only != p) {
                return null;
            }
        }

        return only;
    }
}
