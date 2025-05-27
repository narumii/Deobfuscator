package uwu.narumi.deobfuscator.api.helper;

import org.jetbrains.annotations.Contract;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.FramesProvider;
import uwu.narumi.deobfuscator.api.asm.InsnContext;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Framed instructions stream that gives {@link InsnContext} and computing all frames for you. Also, this class
 * iterate over classes and methods ASYNC, and instructions SYNC. This can really speed up computing frames for methods.
 */
public class FramedInstructionsStream {
  public static FramedInstructionsStream of(Transformer transformer) {
    return of(transformer.scope(), transformer.context());
  }

  public static FramedInstructionsStream of(ClassWrapper scope, Context context) {
    return new FramedInstructionsStream(scope, context);
  }

  private final ClassWrapper scope;
  private final Context context;

  private Function<Stream<ClassWrapper>, Stream<ClassWrapper>> classesStreamModifier = Function.identity();
  private Function<Stream<MethodNode>, Stream<MethodNode>> methodsStreamModifier = Function.identity();
  private Function<Stream<AbstractInsnNode>, Stream<AbstractInsnNode>> instructionsStreamModifier = Function.identity();
  private FramesProvider framesProvider = MethodHelper::analyzeSource;
  private boolean forceSync = false;

  private FramedInstructionsStream(ClassWrapper scope, Context context) {
    this.scope = scope;
    this.context = context;
  }

  @Contract("_ -> this")
  public FramedInstructionsStream editClassesStream(Function<Stream<ClassWrapper>, Stream<ClassWrapper>> stream) {
    this.classesStreamModifier = stream;
    return this;
  }

  @Contract("_ -> this")
  public FramedInstructionsStream editMethodsStream(Function<Stream<MethodNode>, Stream<MethodNode>> stream) {
    this.methodsStreamModifier = stream;
    return this;
  }

  @Contract("_ -> this")
  public FramedInstructionsStream editInstructionsStream(Function<Stream<AbstractInsnNode>, Stream<AbstractInsnNode>> stream) {
    this.instructionsStreamModifier = stream;
    return this;
  }

  @Contract("_ -> this")
  public FramedInstructionsStream framesProvider(FramesProvider framesProvider) {
    this.framesProvider = framesProvider;
    return this;
  }

  @Contract(" -> this")
  public FramedInstructionsStream forceSync() {
    this.forceSync = true;
    return this;
  }

  public void forEach(Consumer<InsnContext> consumer) {
    // Iterate over classes in parallel
    this.classesStreamModifier.apply(this.forceSync ? this.context.scopedClasses(this.scope).stream() : this.context.scopedClasses(this.scope).parallelStream())
        // Iterate over methods in parallel
        .forEach(classWrapper -> this.methodsStreamModifier.apply(this.forceSync ? classWrapper.methods().stream() : classWrapper.methods().parallelStream())
            .forEach(methodNode -> {
              // Skip if no instructions
              if (instructionsStreamModifier.apply(Arrays.stream(methodNode.instructions.toArray())).findAny().isEmpty()) return;

              // Get frames of the method
              MethodContext methodContext = MethodContext.of(classWrapper, methodNode, this.framesProvider);

              // Iterate over instructions SYNC
              instructionsStreamModifier.apply(Arrays.stream(methodNode.instructions.toArray()))
                  .forEach(insn -> {
                    InsnContext insnContext = methodContext.at(insn);
                    // Check if frame exists
                    if (insnContext.frame() == null) return;

                    // Pass instruction context to consumer
                    consumer.accept(insnContext);
                  });
            }));
  }
}
