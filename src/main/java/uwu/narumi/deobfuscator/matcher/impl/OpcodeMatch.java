package uwu.narumi.deobfuscator.matcher.impl;

import org.objectweb.asm.tree.AbstractInsnNode;

import uwu.narumi.deobfuscator.matcher.IMatch;

public class OpcodeMatch implements IMatch {
	int opcode;
	
	public OpcodeMatch(int opcode) {
		this.opcode = opcode;
	}
	
	public boolean match(AbstractInsnNode insn) {
		return insn != null && insn.getOpcode() == opcode;
	}
}
