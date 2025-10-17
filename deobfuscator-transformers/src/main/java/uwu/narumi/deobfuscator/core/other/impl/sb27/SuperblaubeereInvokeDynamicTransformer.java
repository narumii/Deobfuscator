package uwu.narumi.deobfuscator.core.other.impl.sb27;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.MatchContext;
import uwu.narumi.deobfuscator.api.asm.matcher.group.SequenceMatch;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.*;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.*;
import java.util.regex.Pattern;

public class SuperblaubeereInvokeDynamicTransformer extends Transformer {

  Match indyMatch = SequenceMatch.of(
      FieldMatch.getStatic().capture("type"),
      NumberMatch.numInteger().capture("position"),
      Match.of(ctx -> ctx.insn().isType()).or(StringMatch.of()).capture("ldc"),
      OpcodeMatch.of(AASTORE)
  );

  private final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d+$");

  @Override
  protected void transform() throws Exception {
    context().classes().forEach(classWrapper -> {
      Map<Integer, String> callInfos = new HashMap<>();
      Map<Integer, Type> types = new HashMap<>();

      Set<MethodNode> methodsToRemove = new HashSet<>();
      Set<FieldInsnNode> fieldsToRemove = new HashSet<>();

      classWrapper.methods().forEach(methodNode -> {
        MethodContext methodContext = MethodContext.of(classWrapper, methodNode);

        if (isAccess(methodNode.access, ACC_PRIVATE, ACC_STATIC) && methodNode.desc.equals("()V")) {
          indyMatch.findAllMatches(methodContext).forEach(matchContext -> {
            FieldInsnNode fieldInsnNode = matchContext.captures().get("type").insn().asFieldInsn();
            int position = matchContext.captures().get("position").insn().asInteger();
            AbstractInsnNode ldc = matchContext.captures().get("ldc").insn();
            if (ldc.isType()) {
              types.put(position, ldc.asType());
            } else {
              callInfos.put(position, ldc.asString());
            }
            fieldsToRemove.add(fieldInsnNode);
            methodsToRemove.add(methodNode);
            markChange();
          });
        }
      });

      classWrapper.methods().forEach(methodNode -> {
        MethodContext methodContext = MethodContext.of(classWrapper, methodNode);
        Match indyCallMatch = Match.of(ctx -> ctx.insn() instanceof InvokeDynamicInsnNode indy && INTEGER_PATTERN.matcher(indy.name).matches() && callInfos.containsKey(Integer.parseInt(indy.name)));
        indyCallMatch.findAllMatches(methodContext).forEach(matchContext -> {
          InvokeDynamicInsnNode node = matchContext.insn().asInvokeDynamicInsn();
          String[] parts = callInfos.get(Integer.parseInt(node.name)).split(":");
          String owner = parts[0].replace('.', '/');
          String name = parts[1];
          String desc = parts[2];
          int type = parts[3].length();

          Type fieldType = null;
          if (type > 2) {
            fieldType = types.get(Integer.parseInt(desc));
            if (fieldType == null) return;
          }
          switch (type) {
            case 2:
              methodNode.instructions.set(node, new MethodInsnNode(INVOKEVIRTUAL, owner, name, desc, false));
              break;
            case 3:
              methodNode.instructions.set(node, new FieldInsnNode(GETFIELD, owner, name, fieldType.getDescriptor()));
              break;
            case 4:
              methodNode.instructions.set(node, new FieldInsnNode(GETSTATIC, owner, name, fieldType.getDescriptor()));
              break;
            case 5:
              methodNode.instructions.set(node, new FieldInsnNode(PUTFIELD, owner, name, fieldType.getDescriptor()));
              break;
            default:
              if (type <= 2) {
                methodNode.instructions.set(node, new MethodInsnNode(INVOKESTATIC, owner, name, desc, false));
              } else {
                methodNode.instructions.set(node, new FieldInsnNode(PUTSTATIC, owner, name, fieldType.getDescriptor()));
              }
              break;
          }
        });
      });
      findClInit(classWrapper.classNode()).ifPresent(clinit -> {
        MethodContext methodContext = MethodContext.of(classWrapper, clinit);
        Match.of(ctx -> {
          if (ctx.insn() instanceof MethodInsnNode node) {
            if (node.getOpcode() == INVOKESTATIC &&
                node.owner.equals(classWrapper.name()) &&
                methodsToRemove.stream().anyMatch(method -> method.name.equals(node.name) && method.desc.equals(node.desc))) {
                return true;
            }
          }
          return false;
        }).findAllMatches(methodContext).forEach(MatchContext::removeAll);
      });

      fieldsToRemove.forEach(fieldInsnNode -> classWrapper.fields().removeIf(fieldNode -> fieldNode.name.equals(fieldInsnNode.name)
          && fieldNode.desc.equals(fieldInsnNode.desc)));
      classWrapper.methods().removeAll(methodsToRemove);
      classWrapper.methods().removeIf(methodNode -> methodNode.desc.equals("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"));
    });
  }
}
