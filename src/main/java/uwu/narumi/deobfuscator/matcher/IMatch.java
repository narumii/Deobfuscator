package uwu.narumi.deobfuscator.matcher;

import org.objectweb.asm.tree.AbstractInsnNode;

public interface IMatch {
	public boolean match(AbstractInsnNode insn);
}
