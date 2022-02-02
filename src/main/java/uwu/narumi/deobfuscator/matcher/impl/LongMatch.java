package uwu.narumi.deobfuscator.matcher.impl;

import org.objectweb.asm.tree.AbstractInsnNode;

import uwu.narumi.deobfuscator.helper.ASMHelper;
import uwu.narumi.deobfuscator.matcher.IMatch;

public class LongMatch implements IMatch {
	public boolean match(AbstractInsnNode insn) {
		return insn != null && ASMHelper.isLong(insn);
	}
}
