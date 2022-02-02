package uwu.narumi.deobfuscator.matcher.impl;

import org.objectweb.asm.tree.AbstractInsnNode;

import uwu.narumi.deobfuscator.matcher.IMatch;

public class SkipMatch implements IMatch {
	
	public boolean match(AbstractInsnNode insn) {
		return true;
	}
}