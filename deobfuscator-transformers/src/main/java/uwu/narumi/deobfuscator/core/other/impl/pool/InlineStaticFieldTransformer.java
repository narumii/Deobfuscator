package uwu.narumi.deobfuscator.core.other.impl.pool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.rule.impl.MethodMatch;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

// TODO: Don't inline field if it's overridden from different method than the one where was set
// (account for fucking constants values bruh)
public class InlineStaticFieldTransformer extends Transformer {

  private static final Object NULL = new Object();

  private static final Match UNBOX =
      MethodMatch.invokeVirtual()
          .name(
              "intValue",
              "longValue",
              "shortValue",
              "byteValue",
              "floatValue",
              "doubleValue",
              "charValue",
              "booleanValue");

  private static final Set<String> DESCRIPTORS =
      Set.of(
          "I",
          "S",
          "C",
          "B",
          "Z",
          "J",
          "F",
          "D",
          "Ljava/lang/Number;",
          "Ljava/lang/Integer;",
          "Ljava/lang/Float;",
          "Ljava/lang/Short;",
          "Ljava/lang/Double;",
          "Ljava/lang/Byte;",
          "Ljava/lang/Long;",
          "Ljava/lang/Boolean;",
          "Ljava/lang/Character;",
          "Ljava/lang/Object;",
          "Ljava/lang/String;",
          "Ljava/lang/Class;");

  private final boolean scanOnlyStaticBlock;
  private final boolean scanFieldsConstantValue;
  private final AtomicInteger index = new AtomicInteger();

  private final AtomicInteger inline = new AtomicInteger();

  public InlineStaticFieldTransformer() {
    this(true, false);
  }

  public InlineStaticFieldTransformer(
      boolean scanOnlyStaticBlock, boolean scanFieldsConstantValue) {
    this.scanOnlyStaticBlock = scanOnlyStaticBlock;
    this.scanFieldsConstantValue = scanFieldsConstantValue;
  }

  @Override
  protected boolean transform(ClassWrapper scope, Context context) throws Exception {
    context
        .classes(scope)
        .forEach(
            classWrapper -> {
              if (scanFieldsConstantValue) {
                classWrapper.fields().stream()
                    .filter(fieldNode -> isAccess(fieldNode.access, ACC_STATIC))
                    .filter(fieldNode -> fieldNode.value != null)
                    .forEach(
                        fieldNode -> {
                          classWrapper.getFieldCache().set(fieldNode, fieldNode.value);
                          index.incrementAndGet();
                        });

                //                classWrapper.fields().removeIf(fieldNode -> fieldNode.value !=
                // null && NUMBER_DESCRIPTORS.contains(fieldNode.desc));
              }

              if (scanOnlyStaticBlock) {
                classWrapper
                    .findClInit()
                    .ifPresent(staticBlock -> extractNumbers(staticBlock, context));
              } else {
                classWrapper.methods().forEach(methodNode -> extractNumbers(methodNode, context));
              }
            });

    List<FieldRef> toRemove = new ArrayList<>();

    LOGGER.info("Collected {} numbers from {} classes", index.get(), context.classes().size());
    context.classes(scope).stream()
        .flatMap(classWrapper -> classWrapper.methods().stream())
        .forEach(
            methodNode ->
                Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node.getOpcode() == GETSTATIC)
                    .map(FieldInsnNode.class::cast)
                    .filter(node -> context.getClasses().containsKey(node.owner))
                    .forEach(
                        node -> {
                          ClassWrapper owner = context.getClasses().get(node.owner);
                          Object value = owner.getFieldCache().get(node);
                          if (value == null) return;

                          if (node.next().matches(UNBOX)) {
                            methodNode.instructions.remove(node.next());
                          }

                          if (value instanceof String || value instanceof Type) {
                            methodNode.instructions.set(node, new LdcInsnNode(value));
                          } else if (value == NULL) {
                            methodNode.instructions.set(node, new InsnNode(ACONST_NULL));
                          } else if (value instanceof Number number) {
                            methodNode.instructions.set(node, getNumber(number));
                          } else if (value instanceof Boolean bool) {
                            methodNode.instructions.set(node, getNumber(bool ? 1 : 0));
                          } else if (value instanceof Character character) {
                            methodNode.instructions.set(node, getNumber(character));
                          }

                          FieldRef fieldRef = new FieldRef(node.owner, node.name, node.desc);
                          if (!toRemove.contains(fieldRef)) {
                            toRemove.add(fieldRef);
                          }

                          inline.incrementAndGet();
                        }));

    // Cleanup
    toRemove.forEach(fieldRef -> {
      ClassWrapper owner = context.get(fieldRef.owner).get();
      owner.fields().removeIf(fieldNode -> fieldNode.name.equals(fieldRef.name) && fieldNode.desc.equals(fieldRef.desc));
    });

    //        values.clear();
    LOGGER.info("Inlined {} numbers in {} classes", inline.get(), context.classes().size());

    return inline.get() > 0;
  }

  private void extractNumbers(MethodNode methodNode, Context context) {
    Arrays.stream(methodNode.instructions.toArray())
        .filter(node -> node.getOpcode() == PUTSTATIC)
        .filter(
            node ->
                node.previous().isConstant()
                    || (node.previous().previous().isConstant()
                    && node.previous().getOpcode() == INVOKESTATIC))
        .map(FieldInsnNode.class::cast)
        .filter(node -> DESCRIPTORS.contains(node.desc))
        .filter(node -> context.getClasses().containsKey(node.owner))
        .forEach(
            node -> {
              ClassWrapper owner = context.getClasses().get(node.owner);
              set(
                  owner,
                  node,
                  node.previous().isConstant() ? node.previous() : node.previous().previous());
              //                    methodNode.instructions.remove(node.previous());
              //                    methodNode.instructions.remove(node);
              //                    owner.fields().removeIf(fieldNode ->
              // fieldNode.name.equals(node.name) && fieldNode.desc.equals(node.desc));
              index.incrementAndGet();
            });
  }

  private void set(ClassWrapper owner, FieldInsnNode fieldNode, AbstractInsnNode valueNode) {
    if (valueNode.isInteger()) {
      owner.getFieldCache().set(fieldNode, valueNode.asInteger());
    } else if (valueNode.isLong()) {
      owner.getFieldCache().set(fieldNode, valueNode.asLong());
    } else if (valueNode.isFloat()) {
      owner.getFieldCache().set(fieldNode, valueNode.asFloat());
    } else if (valueNode.isDouble()) {
      owner.getFieldCache().set(fieldNode, valueNode.asDouble());
    } else if (valueNode.isString()) {
      owner.getFieldCache().set(fieldNode, valueNode.asString());
    } else if (valueNode.isType()) {
      owner.getFieldCache().set(fieldNode, valueNode.asType());
    } else if (valueNode.isNull()) {
      owner.getFieldCache().set(fieldNode, NULL);
    }
  }

  private record FieldRef(String owner, String name, String desc) {
  }
}
