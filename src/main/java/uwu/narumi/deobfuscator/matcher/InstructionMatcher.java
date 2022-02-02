package uwu.narumi.deobfuscator.matcher;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LineNumberNode;
/*
 * i want to kill myself
 */
public class InstructionMatcher {
	
	private IMatch[] insns;
	
	public InstructionMatcher(IMatch[] insns) {
		this.insns = insns;
	}
	
	public AbstractInsnNode[] collectMatchedInstructions(AbstractInsnNode node) {
		AbstractInsnNode[] collected = new AbstractInsnNode[insns.length];
		
		InsnList insnList = generate(node, insns.length);
		if(insnList != null && insnList.size() != 0 && insns.length <= insnList.size()) {
			AbstractInsnNode currentInstruction = insnList.getFirst();
			for (int i = 0; i < insns.length; i++) {
				collected[i] = currentInstruction;
				currentInstruction = currentInstruction.getNext();
			}
		}
		return collected;
	}
	
	public boolean isMatch(AbstractInsnNode node) {
		if(node == null) return false;
		InsnList insnList = generate(node, insns.length);
		if(insnList == null || insnList.size() == 0 && insns.length > insnList.size()) return false;
		AbstractInsnNode currentInstruction = insnList.getFirst();
		for (int i = 0; i < insns.length; i++) {
			IMatch insn = insns[i];
			if(!insn.match(currentInstruction)) return false;
			currentInstruction = currentInstruction.getNext();
		}
		return true;
	}
	
	private InsnList generate(AbstractInsnNode node, int offset) {
		try {
			InsnList insnList = new InsnList();
			AbstractInsnNode currentInsn = node;
			for (int i = 0; i <= offset; i++) {
				insnList.add(currentInsn);
				currentInsn = currentInsn.getNext();
			}
			return insnList;
		} catch(Exception e) {
			return new InsnList();
		}
	}

}
