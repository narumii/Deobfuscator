package uwu.narumi.deobfuscator.api.asm;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;

import java.util.Map;

@FunctionalInterface
public interface FramesProvider {
  Map<AbstractInsnNode, Frame<OriginalSourceValue>> compute(ClassNode classNode, MethodNode methodNode);
}
