package uwu.narumi.deobfuscator.core.other.impl.pool;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

// TODO: Don't inline value from array if it's overridden from different method than the one where
// array was created
// TODO: Don't inline array if it's overridden from different method than the where array was
// created
// TODO: Rewrite it entirely
public class InlineStaticArrayFieldTransformer extends Transformer {
  @Override
  protected void transform() throws Exception {
    throw new UnsupportedOperationException("Not implemented yet");
  }

//  private static final Object NULL = new Object();
//
//  private static final Match UNBOX =
//      MethodMatch.invokeVirtual()
//          .name(
//              "intValue",
//              "longValue",
//              "shortValue",
//              "byteValue",
//              "floatValue",
//              "doubleValue",
//              "charValue",
//              "booleanValue");
//
//  private static final Match SET_STATIC_MATCHER =
//      AnyMatch.of(
//          FieldMatch.putStatic().desc("[Ljava/lang/Number;"),
//          FieldMatch.putStatic().desc("[Ljava/lang/Integer;"),
//          FieldMatch.putStatic().desc("[Ljava/lang/Float;"),
//          FieldMatch.putStatic().desc("[Ljava/lang/Short;"),
//          FieldMatch.putStatic().desc("[Ljava/lang/Double;"),
//          FieldMatch.putStatic().desc("[Ljava/lang/Byte;"),
//          FieldMatch.putStatic().desc("[Ljava/lang/Long;"),
//          FieldMatch.putStatic().desc("[Ljava/lang/Boolean;"),
//          FieldMatch.putStatic().desc("[Ljava/lang/Character;"),
//          FieldMatch.putStatic().desc("[Ljava/lang/Object;"),
//          FieldMatch.putStatic().desc("[Ljava/lang/String;"),
//          FieldMatch.putStatic().desc("[Ljava/lang/Class;"),
//          FieldMatch.putStatic().desc("[I"),
//          FieldMatch.putStatic().desc("[J"),
//          FieldMatch.putStatic().desc("[F"),
//          FieldMatch.putStatic().desc("[D"),
//          FieldMatch.putStatic().desc("[S"),
//          FieldMatch.putStatic().desc("[C"),
//          FieldMatch.putStatic().desc("[B"),
//          FieldMatch.putStatic().desc("[Z"));
//
//  private static final InstructionMatcher ARRAY_INIT_MATCHER =
//      InstructionMatcher.builder()
//          .matches(
//              IntegerMatch.of(),
//              AnyMatch.of(
//                  TypeMatch.of(ANEWARRAY, "java/lang/Number"),
//                  TypeMatch.of(ANEWARRAY, "java/lang/Integer"),
//                  TypeMatch.of(ANEWARRAY, "java/lang/Float"),
//                  TypeMatch.of(ANEWARRAY, "java/lang/Short"),
//                  TypeMatch.of(ANEWARRAY, "java/lang/Double"),
//                  TypeMatch.of(ANEWARRAY, "java/lang/Byte"),
//                  TypeMatch.of(ANEWARRAY, "java/lang/Long"),
//                  TypeMatch.of(ANEWARRAY, "java/lang/Boolean"),
//                  TypeMatch.of(ANEWARRAY, "java/lang/Character"),
//                  TypeMatch.of(ANEWARRAY, "java/lang/Object"),
//                  TypeMatch.of(ANEWARRAY, "java/lang/String"),
//                  TypeMatch.of(ANEWARRAY, "java/lang/Class"),
//                  IntInsnMatch.of(NEWARRAY, T_BOOLEAN),
//                  IntInsnMatch.of(NEWARRAY, T_INT),
//                  IntInsnMatch.of(NEWARRAY, T_BYTE),
//                  IntInsnMatch.of(NEWARRAY, T_CHAR),
//                  IntInsnMatch.of(NEWARRAY, T_FLOAT),
//                  IntInsnMatch.of(NEWARRAY, T_DOUBLE),
//                  IntInsnMatch.of(NEWARRAY, T_LONG),
//                  IntInsnMatch.of(NEWARRAY, T_SHORT)))
//          .build();
//
//  private static final InstructionMatcher ARRAY_STORE_MATCHER =
//      InstructionMatcher.builder()
//          .matches(
//              FieldMatch.getStatic(),
//              IntegerMatch.of(),
//              AnyMatch.of(
//                  IntegerMatch.of(),
//                  LongMatch.of(),
//                  FloatMatch.of(),
//                  DoubleMatch.of(),
//                  TypeMatch.of(),
//                  StringMatch.of(),
//                  OpcodeMatch.of(ACONST_NULL)))
//          .build();
//
//  private static final InstructionMatcher ARRAY_DUP_STORE_MATCHER =
//      InstructionMatcher.builder()
//          .matches(
//              OpcodeMatch.of(DUP),
//              IntegerMatch.of(),
//              AnyMatch.of(
//                  IntegerMatch.of(),
//                  LongMatch.of(),
//                  FloatMatch.of(),
//                  DoubleMatch.of(),
//                  TypeMatch.of(),
//                  StringMatch.of(),
//                  OpcodeMatch.of(ACONST_NULL)))
//          .build();
//
//  private static final InstructionMatcher LOAD_VALUE_MATCHER =
//      InstructionMatcher.builder()
//          .matches(
//              AnyMatch.of(
//                  FieldMatch.getStatic().desc("[Ljava/lang/Number;"),
//                  FieldMatch.getStatic().desc("[Ljava/lang/Integer;"),
//                  FieldMatch.getStatic().desc("[Ljava/lang/Float;"),
//                  FieldMatch.getStatic().desc("[Ljava/lang/Short;"),
//                  FieldMatch.getStatic().desc("[Ljava/lang/Double;"),
//                  FieldMatch.getStatic().desc("[Ljava/lang/Byte;"),
//                  FieldMatch.getStatic().desc("[Ljava/lang/Long;"),
//                  FieldMatch.getStatic().desc("[Ljava/lang/Boolean;"),
//                  FieldMatch.getStatic().desc("[Ljava/lang/Character;"),
//                  FieldMatch.getStatic().desc("[Ljava/lang/Object;"),
//                  FieldMatch.getStatic().desc("[Ljava/lang/String;"),
//                  FieldMatch.getStatic().desc("[Ljava/lang/Class;"),
//                  FieldMatch.getStatic().desc("[I"),
//                  FieldMatch.getStatic().desc("[J"),
//                  FieldMatch.getStatic().desc("[F"),
//                  FieldMatch.getStatic().desc("[D"),
//                  FieldMatch.getStatic().desc("[S"),
//                  FieldMatch.getStatic().desc("[C"),
//                  FieldMatch.getStatic().desc("[B"),
//                  FieldMatch.getStatic().desc("[Z")),
//              IntegerMatch.of(),
//              AnyMatch.of(
//                  OpcodeMatch.of(AALOAD),
//                  OpcodeMatch.of(IALOAD),
//                  OpcodeMatch.of(LALOAD),
//                  OpcodeMatch.of(FALOAD),
//                  OpcodeMatch.of(DALOAD),
//                  OpcodeMatch.of(CALOAD),
//                  OpcodeMatch.of(SALOAD),
//                  OpcodeMatch.of(BALOAD)))
//          .build();
//
//  private final boolean scanOnlyStaticBlock;
//  private final AtomicInteger index = new AtomicInteger();
//
//  public InlineStaticArrayFieldTransformer() {
//    this(false);
//  }
//
//  public InlineStaticArrayFieldTransformer(boolean scanOnlyStaticBlock) {
//    this.scanOnlyStaticBlock = scanOnlyStaticBlock;
//  }
//
//  @Override
//  protected void transform(ClassWrapper scope, Context context) throws Exception {
//    context
//        .classes(scope)
//        .forEach(
//            classWrapper -> {
//              if (scanOnlyStaticBlock) {
//                classWrapper
//                    .findClInit()
//                    .ifPresent(staticBlock -> extractData(classWrapper, staticBlock, context));
//              } else {
//                List.copyOf(classWrapper.methods())
//                    .forEach(methodNode -> extractData(classWrapper, methodNode, context));
//              }
//            });
//
//    LOGGER.info("Collected {} objects from {} classes", index.get(), context.classes().size());
//    context
//        .classes(scope)
//        .forEach(
//            classWrapper -> {
//              classWrapper
//                  .methods()
//                  .forEach(
//                      methodNode ->
//                          LOAD_VALUE_MATCHER
//                              .bind(classWrapper.getClassNode(), methodNode)
//                              .modifyAll(
//                                  (result, method) -> {
//                                    FieldInsnNode node = result.get(0);
//                                    if (!classWrapper.getFieldCache().has(node)
//                                        || !classWrapper.getFieldCache().isPresent(node)) return;
//
//                                    Object value =
//                                        classWrapper
//                                            .getFieldCache()
//                                            .<Map<Integer, Object>>get(node)
//                                            .get(result.get(1).asInteger());
//                                    if (value == null) return;
//
//                                    if (value instanceof String || value instanceof Type) {
//                                      method.instructions.insertBefore(
//                                          result.start(), new LdcInsnNode(value));
//                                    } else if (value == NULL) {
//                                      method.instructions.insertBefore(
//                                          result.start(), new InsnNode(ACONST_NULL));
//                                    } else if (value instanceof Number number) {
//                                      method.instructions.insertBefore(
//                                          result.start(), getNumber(number));
//                                      if (result.end().next().matches(UNBOX))
//                                        method.instructions.remove(result.end().next());
//                                    } else if (value instanceof Boolean bool) {
//                                      method.instructions.insertBefore(
//                                          result.start(), getNumber(bool ? 1 : 0));
//                                      if (result.end().next().matches(UNBOX))
//                                        method.instructions.remove(result.end().next());
//                                    } else if (value instanceof Character character) {
//                                      method.instructions.insertBefore(
//                                          result.start(), getNumber(character));
//                                      if (result.end().next().matches(UNBOX))
//                                        method.instructions.remove(result.end().next());
//                                    }
//
//                                    result.remove();
//                                    this.markChange();
//                                  }));
//            });
//
//    LOGGER.info("Inlined {} objects in {} classes", this.getChangesCount(), context.classes().size());
//  }
//
//  private void extractData(ClassWrapper classWrapper, MethodNode methodNode, Context context) {
//    Arrays.stream(methodNode.instructions.toArray())
//        .filter(node1 -> SET_STATIC_MATCHER.test(node1, , ))
//        .map(FieldInsnNode.class::cast)
//        .filter(node -> context.get(node.owner).isPresent())
//        .filter(node -> !ARRAY_INIT_MATCHER.match(node.previous(3)))
//        .forEach(
//            node -> {
//              ClassWrapper owner = context.get(node.owner).get();
//              {
//                for (Result result : ARRAY_STORE_MATCHER.bind(classWrapper.getClassNode(), methodNode).collect()) {
//                  FieldInsnNode fieldNode = result.get(0);
//                  if (!fieldNode.owner.equals(node.owner)
//                      || !fieldNode.name.equals(node.name)
//                      || !fieldNode.desc.equals(node.desc)) continue;
//
//                  int position = result.get(1).asInteger();
//                  AbstractInsnNode valueNode = result.get(2);
//                  Object value = putValue(owner, fieldNode, position, valueNode);
//                  if (value instanceof Number
//                      || value instanceof Boolean
//                      || value instanceof Character) {
//                    //
//                    // methodNode.instructions.remove(result.end().next().next()); //boxing
//                  }
//                  //                        methodNode.instructions.remove(result.end().next());
//                  // //aastore
//                  //                        result.remove();
//                  index.incrementAndGet();
//                }
//
//                //                    methodNode.instructions.remove(node.getPrevious(2));
//                //                    methodNode.instructions.remove(node.getPrevious());
//                //                    methodNode.instructions.remove(node);
//                //                    owner.fields().removeIf(fieldNode ->
//                // fieldNode.name.equals(node.name) && fieldNode.desc.equals(node.desc));
//              }
//
//              {
//                AbstractInsnNode last =
//                    node.walkPreviousUntil(
//                        ARRAY_INIT_MATCHER::match,
//                        insn ->
//                            ARRAY_DUP_STORE_MATCHER.match(
//                                insn,
//                                result -> {
//                                  int position = result.get(1).asInteger();
//                                  AbstractInsnNode valueNode = result.get(2);
//                                  Object value = putValue(owner, node, position, valueNode);
//                                  index.incrementAndGet();
//                                }));
//              }
//            });
//
//    if (!methodNode.name.startsWith("<")
//        && methodNode.desc.equals("()V")
//        && isAccess(methodNode.access, ACC_STATIC)
//        && Arrays.stream(methodNode.instructions.toArray())
//                .filter(node -> !(node instanceof LabelNode))
//                .filter(node -> !(node instanceof FrameNode))
//                .filter(node -> !(node instanceof LineNumberNode))
//                .filter(node -> node.getOpcode() != NOP)
//                .count()
//            <= 1) {
//
//      classWrapper.methods().remove(methodNode);
//      classWrapper
//          .methods()
//          .forEach(
//              method ->
//                  Arrays.stream(method.instructions.toArray())
//                      .filter(node -> node instanceof MethodInsnNode)
//                      .map(MethodInsnNode.class::cast)
//                      .filter(node -> node.owner.equals(classWrapper.name()))
//                      .filter(node -> node.name.equals(methodNode.name))
//                      .filter(node -> node.desc.equals(methodNode.desc))
//                      .forEach(method.instructions::remove));
//    }
//  }
//
//  private Object putValue(
//      ClassWrapper owner, FieldInsnNode fieldNode, int position, AbstractInsnNode valueNode) {
//    Object value = null;
//    if (valueNode.isInteger()) {
//      value = valueNode.asInteger();
//    } else if (valueNode.isLong()) {
//      value = valueNode.asLong();
//    } else if (valueNode.isFloat()) {
//      value = valueNode.asFloat();
//    } else if (valueNode.isDouble()) {
//      value = valueNode.asDouble();
//    } else if (valueNode.isString()) {
//      value = valueNode.asString();
//    } else if (valueNode.isType()) {
//      value = valueNode.asType();
//    } else if (valueNode.getOpcode() == ACONST_NULL) {
//      value = NULL;
//    }
//
//    if (value == null) return value;
//
//    owner
//        .getFieldCache()
//        .getOrCompute(fieldNode, new HashMap<Integer, Object>())
//        .put(position, value);
//    return value;
//  }
}
