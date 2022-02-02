package uwu.narumi.deobfuscator.matcher.impl;

import org.objectweb.asm.tree.AbstractInsnNode;

import uwu.narumi.deobfuscator.matcher.IMatch;

public class ClassMatch implements IMatch {
	Class<?> clazz;
	
	public ClassMatch(Class<?> clazz) {
		this.clazz = clazz;
	}
	
	public boolean match(AbstractInsnNode insn) {
		return insn != null && insn.getClass().equals(clazz);
	}
}

