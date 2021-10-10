package uwu.narumi.deobfuscator.transformer.impl.caesium;

import java.util.ArrayList;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;

import uwu.narumi.deobfuscator.Deobfuscator;
import uwu.narumi.deobfuscator.transformer.Transformer;

public class CaesiumFlowTransformer extends Transformer {

	@Override
	public void transform(Deobfuscator deobfuscator) throws Exception {
		int removed = 0, removedFields = 0;
		ArrayList<FieldInsnNode> toRemove = new ArrayList<>();
		for(ClassNode classNode : deobfuscator.classes()) {
			for(MethodNode methodNode : classNode.methods) {
				for(AbstractInsnNode insn : methodNode.instructions.toArray()) {
					if(!check(insn, GETSTATIC)) continue;
					if(!check(insn.getNext(), JumpInsnNode.class)) continue;
					if(!check(insn.getNext().getNext(), ACONST_NULL)) continue;
					if(!check(insn.getNext().getNext().getNext(), ATHROW)) continue;
					
					toRemove.add(((FieldInsnNode)insn));
					
					methodNode.instructions.remove(insn.getNext().getNext().getNext());
					methodNode.instructions.remove(insn.getNext().getNext());
					methodNode.instructions.set(insn, new JumpInsnNode(GOTO, ((JumpInsnNode)insn.getNext()).label));
					removed++;
				}
			}
		}
		
		for(FieldInsnNode field : toRemove) {
			ClassNode classNode = deobfuscator.classes().stream().filter(claz -> claz.name.equals(field.owner)).findFirst().orElse(null);
			if(classNode != null) {
				classNode.fields.removeIf(fld -> fld.name.equals(field.name) && fld.desc.equals(field.desc));
				removedFields++;
			}
		}
		
		System.out.println("Removed " + removedFields + " useless fields");
		System.out.println("Removed " + removed + " flow instructions");
	}
	

}
