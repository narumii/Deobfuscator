package uwu.narumi.deobfuscator.core.other.impl.universal;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.objectweb.asm.tree.*;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.data.TriFunction;
import uwu.narumi.deobfuscator.api.helper.AsmMathHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

// Maybe use matcher? idk really
// TODO: Add some light jump emulation, basically rewrite loop to index loop
// AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
// TODO: Fucking variables?????
public class UniversalNumberTransformer extends Transformer {

  private final Set<TriFunction<ClassWrapper, MethodNode, AbstractInsnNode, Boolean>> functions =
      new LinkedHashSet<>();

  private final AtomicInteger resolved = new AtomicInteger();

  @Override
  public void transform(ClassWrapper scope, Context context) throws Exception {
    context
        .classes(scope)
        .forEach(
            classWrapper -> {
              for (MethodNode methodNode : classWrapper.methods()) {
                boolean modified;
                do {
                  modified = false;
                  for (AbstractInsnNode node : methodNode.instructions.toArray()) {
                    if (AsmMathHelper.STRING_LENGTH.test(node)
                        && AsmMathHelper.STRING_LENGTH.invoke(methodNode, node)) {
                      modified = true;
                      resolved.incrementAndGet();
                    } else if (AsmMathHelper.STRING_HASHCODE.test(node)
                        && AsmMathHelper.STRING_HASHCODE.invoke(methodNode, node)) {
                      modified = true;
                      resolved.incrementAndGet();
                    } else if (AsmMathHelper.STRING_TO_INTEGER.test(node)
                        && AsmMathHelper.STRING_TO_INTEGER.invoke(methodNode, node)) {
                      modified = true;
                      resolved.incrementAndGet();
                    } else if (AsmMathHelper.STRING_TO_INTEGER_RADIX.test(node)
                        && AsmMathHelper.STRING_TO_INTEGER_RADIX.invoke(methodNode, node)) {
                      modified = true;
                      resolved.incrementAndGet();
                    } else if (AsmMathHelper.INTEGER_REVERSE.test(node)
                        && AsmMathHelper.INTEGER_REVERSE.invoke(methodNode, node)) {
                      modified = true;
                      resolved.incrementAndGet();
                    } else if (AsmMathHelper.LONG_REVERSE.test(node)
                        && AsmMathHelper.LONG_REVERSE.invoke(methodNode, node)) {
                      modified = true;
                      resolved.incrementAndGet();
                    } else if (AsmMathHelper.FLOAT_TO_BITS.test(node)
                        && AsmMathHelper.FLOAT_TO_BITS.invoke(methodNode, node)) {
                      modified = true;
                      resolved.incrementAndGet();
                    } else if (AsmMathHelper.BITS_TO_FLOAT.test(node)
                        && AsmMathHelper.BITS_TO_FLOAT.invoke(methodNode, node)) {
                      modified = true;
                      resolved.incrementAndGet();
                    } else if (AsmMathHelper.DOUBLE_TO_BITS.test(node)
                        && AsmMathHelper.DOUBLE_TO_BITS.invoke(methodNode, node)) {
                      modified = true;
                      resolved.incrementAndGet();
                    } else if (AsmMathHelper.BITS_TO_DOUBLE.test(node)
                        && AsmMathHelper.BITS_TO_DOUBLE.invoke(methodNode, node)) {
                      modified = true;
                      resolved.incrementAndGet();
                    }

                    if (node.getOpcode() == LCMP
                        && node.previous().isLong()
                        && node.previous().previous().isLong()) {
                      methodNode.instructions.set(
                          node.previous().previous(),
                          getNumber(
                              AsmMathHelper.lcmp(
                                  node.previous().previous().asLong(), node.previous().asLong())));
                      methodNode.instructions.remove(node.previous());
                      methodNode.instructions.remove(node);
                      modified = true;
                      resolved.incrementAndGet();
                    } else if ((node.getOpcode() == FCMPL || node.getOpcode() == FCMPG)
                        && node.previous().isFloat()
                        && node.previous().previous().isFloat()) {
                      methodNode.instructions.set(
                          node.previous().previous(),
                          getNumber(
                              node.getOpcode() == FCMPL
                                  ? AsmMathHelper.fcmpl(
                                      node.previous().previous().asFloat(),
                                      node.previous().asFloat())
                                  : AsmMathHelper.fcmpg(
                                      node.previous().previous().asFloat(),
                                      node.previous().asFloat())));
                      methodNode.instructions.remove(node.previous());
                      methodNode.instructions.remove(node);
                      modified = true;
                      resolved.incrementAndGet();
                    } else if ((node.getOpcode() == DCMPL || node.getOpcode() == DCMPG)
                        && node.previous().isDouble()
                        && node.previous().previous().isDouble()) {
                      methodNode.instructions.set(
                          node.previous().previous(),
                          getNumber(
                              node.getOpcode() == DCMPL
                                  ? AsmMathHelper.dcmpl(
                                      node.previous().previous().asDouble(),
                                      node.previous().asDouble())
                                  : AsmMathHelper.dcmpg(
                                      node.previous().previous().asDouble(),
                                      node.previous().asDouble())));
                      methodNode.instructions.remove(node.previous());
                      methodNode.instructions.remove(node);
                      modified = true;
                      resolved.incrementAndGet();
                    }

                    if (node.isInteger() && node.next() != null) {
                      int number = node.asInteger();
                      switch (node.next().getOpcode()) {
                        case INEG -> {
                          methodNode.instructions.set(node.next(), getNumber(-number));
                          methodNode.instructions.remove(node);
                          modified = true;
                          resolved.incrementAndGet();
                        }
                        case I2B -> {
                          methodNode.instructions.set(node.next(), getNumber(((byte) number)));
                          methodNode.instructions.remove(node);
                          modified = true;
                          resolved.incrementAndGet();
                        }
                        case I2C -> {
                          methodNode.instructions.set(node.next(), getNumber(((char) number)));
                          methodNode.instructions.remove(node);
                          modified = true;
                          resolved.incrementAndGet();
                        }
                        case I2D -> {
                          methodNode.instructions.set(node.next(), getNumber(((double) number)));
                          methodNode.instructions.remove(node);
                          modified = true;
                          resolved.incrementAndGet();
                        }
                        case I2F -> {
                          methodNode.instructions.set(node.next(), getNumber(((float) number)));
                          methodNode.instructions.remove(node);
                          modified = true;
                          resolved.incrementAndGet();
                        }
                        case I2L -> {
                          methodNode.instructions.set(node.next(), getNumber(((long) number)));
                          methodNode.instructions.remove(node);
                          modified = true;
                          resolved.incrementAndGet();
                        }
                        case I2S -> {
                          methodNode.instructions.set(node.next(), getNumber(((short) number)));
                          methodNode.instructions.remove(node);
                          modified = true;
                          resolved.incrementAndGet();
                        }
                      }
                    } else if (node.isLong()) {
                      long number = node.asLong();
                      switch (node.next().getOpcode()) {
                        case LNEG -> {
                          methodNode.instructions.set(node.next(), getNumber(-number));
                          methodNode.instructions.remove(node);
                          modified = true;
                          resolved.incrementAndGet();
                        }
                        case L2D -> {
                          methodNode.instructions.set(node.next(), getNumber(((double) number)));
                          methodNode.instructions.remove(node);
                          modified = true;
                          resolved.incrementAndGet();
                        }
                        case L2F -> {
                          methodNode.instructions.set(node.next(), getNumber(((float) number)));
                          methodNode.instructions.remove(node);
                          modified = true;
                          resolved.incrementAndGet();
                        }
                        case L2I -> {
                          methodNode.instructions.set(node.next(), getNumber(((int) number)));
                          methodNode.instructions.remove(node);
                          modified = true;
                          resolved.incrementAndGet();
                        }
                      }
                    } else if (node.isFloat()) {
                      float number = node.asFloat();
                      switch (node.next().getOpcode()) {
                        case FNEG -> {
                          methodNode.instructions.set(node.next(), getNumber(-number));
                          methodNode.instructions.remove(node);
                          modified = true;
                          resolved.incrementAndGet();
                        }
                        case F2D -> {
                          methodNode.instructions.set(node.next(), getNumber(((double) number)));
                          methodNode.instructions.remove(node);
                          modified = true;
                          resolved.incrementAndGet();
                        }
                        case F2I -> {
                          methodNode.instructions.set(node.next(), getNumber(((int) number)));
                          methodNode.instructions.remove(node);
                          modified = true;
                          resolved.incrementAndGet();
                        }
                        case F2L -> {
                          methodNode.instructions.set(node.next(), getNumber(((long) number)));
                          methodNode.instructions.remove(node);
                          modified = true;
                          resolved.incrementAndGet();
                        }
                      }
                    } else if (node.isDouble()) {
                      double number = node.asDouble();
                      switch (node.next().getOpcode()) {
                        case DNEG -> {
                          methodNode.instructions.set(node.next(), getNumber(-number));
                          methodNode.instructions.remove(node);
                          modified = true;
                          resolved.incrementAndGet();
                        }
                        case D2F -> {
                          methodNode.instructions.set(node.next(), getNumber(((float) number)));
                          methodNode.instructions.remove(node);
                          modified = true;
                          resolved.incrementAndGet();
                        }
                        case D2I -> {
                          methodNode.instructions.set(node.next(), getNumber(((int) number)));
                          methodNode.instructions.remove(node);
                          modified = true;
                          resolved.incrementAndGet();
                        }
                        case D2L -> {
                          methodNode.instructions.set(node.next(), getNumber(((long) number)));
                          methodNode.instructions.remove(node);
                          modified = true;
                          resolved.incrementAndGet();
                        }
                      }
                    }

                    if (node.isInteger()
                        && node.next().isInteger()
                        && node.next().next().isMathOperator()) {
                      methodNode.instructions.set(
                          node.next().next(),
                          getNumber(
                              AsmMathHelper.mathOperation(
                                  node.asInteger(),
                                  node.next().asInteger(),
                                  node.next().next().getOpcode())));
                      methodNode.instructions.remove(node.next());
                      methodNode.instructions.remove(node);
                      modified = true;
                      resolved.incrementAndGet();
                    } else if (node.isLong()
                        && node.next().isLong()
                        && node.next().next().isMathOperator()) {
                      methodNode.instructions.set(
                          node.next().next(),
                          getNumber(
                              AsmMathHelper.mathOperation(
                                  node.asLong(),
                                  node.next().asLong(),
                                  node.next().next().getOpcode())));
                      methodNode.instructions.remove(node.next());
                      methodNode.instructions.remove(node);
                      modified = true;
                      resolved.incrementAndGet();
                    } else if (node.isFloat()
                        && node.next().isFloat()
                        && node.next().next().isMathOperator()) {
                      methodNode.instructions.set(
                          node.next().next(),
                          getNumber(
                              AsmMathHelper.mathOperation(
                                  node.asFloat(),
                                  node.next().asFloat(),
                                  node.next().next().getOpcode())));
                      methodNode.instructions.remove(node.next());
                      methodNode.instructions.remove(node);
                      modified = true;
                      resolved.incrementAndGet();
                    } else if (node.isDouble()
                        && node.next().isDouble()
                        && node.next().next().isMathOperator()) {
                      methodNode.instructions.set(
                          node.next().next(),
                          getNumber(
                              AsmMathHelper.mathOperation(
                                  node.asDouble(),
                                  node.next().asDouble(),
                                  node.next().next().getOpcode())));
                      methodNode.instructions.remove(node.next());
                      methodNode.instructions.remove(node);
                      modified = true;
                      resolved.incrementAndGet();
                    }

                    if ((node.getOpcode() == IFNULL || node.getOpcode() == IFNONNULL)
                        && node.previous().getOpcode() == ACONST_NULL) {
                      methodNode.instructions.remove(node.previous());
                      if (node.getOpcode() != IFNULL
                      //                                || methodNode.instructions.indexOf(node) >
                      // methodNode.instructions.indexOf(node.asJump().label)
                      ) {
                        methodNode.instructions.remove(node);
                      } else {
                        methodNode.instructions.set(
                            node, new JumpInsnNode(GOTO, node.asJump().label));
                      }

                      modified = true;
                      resolved.incrementAndGet();
                    } else if (node.conditionStackSize() == 2
                        && node.previous().isInteger()
                        && node.previous().previous().isInteger()) {
                      boolean result =
                          AsmMathHelper.condition(
                              node.previous().previous().asInteger(),
                              node.previous().asInteger(),
                              node.getOpcode());

                      methodNode.instructions.remove(node.previous().previous());
                      methodNode.instructions.remove(node.previous());
                      if (!result
                      //                                || methodNode.instructions.indexOf(node) >
                      // methodNode.instructions.indexOf(node.asJump().label)
                      ) {
                        methodNode.instructions.remove(node);
                      } else {
                        methodNode.instructions.set(
                            node, new JumpInsnNode(GOTO, node.asJump().label));
                      }

                      modified = true;
                      resolved.incrementAndGet();
                    } else if (node.conditionStackSize() == 1 && node.previous().isInteger()) {
                      boolean result =
                          AsmMathHelper.condition(node.previous().asInteger(), node.getOpcode());
                      methodNode.instructions.remove(node.previous());
                      if (!result
                      //                                || methodNode.instructions.indexOf(node) >
                      // methodNode.instructions.indexOf(node.asJump().label)
                      ) {
                        methodNode.instructions.remove(node);
                      } else {
                        methodNode.instructions.set(
                            node, new JumpInsnNode(GOTO, node.asJump().label));
                      }

                      modified = true;
                      resolved.incrementAndGet();
                    }

                    for (TriFunction<ClassWrapper, MethodNode, AbstractInsnNode, Boolean> function :
                        functions) {
                      if (function.apply(classWrapper, methodNode, node)) {
                        modified = true;
                        resolved.incrementAndGet();
                      }
                    }
                  }
                } while (modified);
              }
            });

    LOGGER.info(
        "Simplified {} number operations in {} classes", resolved.get(), context.classes().size());
  }

  public void registerFunction(
      TriFunction<ClassWrapper, MethodNode, AbstractInsnNode, Boolean> function) {
    functions.add(function);
  }
}
