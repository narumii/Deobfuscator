package uwu.narumi.deobfuscator.matcher.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import uwu.narumi.deobfuscator.matcher.IMatch;

public class MethodMatch implements IMatch {
	int opcode;
	String owner, desc, name;
	
	public MethodMatch(int opcode, String owner, String desc, String name) {
		this.opcode = opcode;
		this.owner = owner;
		this.desc = desc;
		this.name = name;
	}

	public boolean match(AbstractInsnNode insn) {
		if(insn == null || !(insn instanceof MethodInsnNode)) return false;
		MethodInsnNode min = (MethodInsnNode) insn;
		return min.getOpcode() == opcode && min.owner.equals(owner) && min.desc.equals(desc) && min.name.equals(name);
	}
}