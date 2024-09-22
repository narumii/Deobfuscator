# Contributing
Welcome! We are glad that you want to contribute to our deobfuscator.

## ✨ Best practises
- Add comments in complex areas. ASM is hard, so let's make it easier for others to understand what is happening in code (especially inside transformers).
- Upload obfuscated test class sample. We highly recommend tests for your transformers. When someone wants to rewrite some part of the code, then these tests are helping us that we won't break anything.

## 🪜 Project structure
The project is structured as follows:
- [`deobfuscator-api`](./deobfuscator-api) - The API for the deobfuscator.
- [`deobfuscator-impl`](./deobfuscator-impl) - The main deobfuscator runner.
- [`deobfuscator-transformers`](./deobfuscator-transformers) - Transformers for the deobfuscator.
- [`reverse-engineering`](./deobfuscator-impl/src/test/java/reverseengineering) - A place where you can throw your reverse-engineered classes. More info [here](./deobfuscator-impl/src/test/java/reverseengineering/README.md)
- [`testData`](./testData) - Tests for transformers
  - [`src/java`](./testData/src/java) - You can write your java code to test transformers
  - [`compiled/custom-classes`](./testData/compiled/custom-classes) - Compiled classes to test transformers. You can throw here classes from your obfuscated jars.
  - [`compiled/custom-jars`](./testData/compiled/custom-jars) - Jars to test transformers. You can throw here your obfuscated jars.
  - `deobfuscated` - Raw classes after a deobfuscation process. Useful when debugging.
  - [`results`](./testData/results) - Expected results that are auto-generated decompiled java code.
- [`TestDeobfuscation.java`](./deobfuscator-impl/src/test/java/uwu/narumi/deobfuscator/TestDeobfuscation.java) - Class where each test sample is registered.
- [`Bootstrap.java`](./deobfuscator-impl/src/test/java/Bootstrap.java) - Class where you can run deobfuscator manually.

## 🧰 Recommended tools
- [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) - IDE for Java development
- [Recaf](https://github.com/Col-E/Recaf) - Modern java bytecode editor. Use it to analyze obfuscated classes.

## 🔢 Some basics about java bytecode
[First of all](https://www.youtube.com/watch?v=TrHabuoQf7s) you need to learn some basics about java bytecode. The best way to learn it is to write an example java code (start with "hello world" program), compile it and throw the compiled jar to Recaf. Then find your class, right click, and click `Edit` -> `Edit class in assembler`. Here you can see your java bytecode. Try to compare it with your written java code, and find similarities, like how method invocation is done, variable accesses, math operations, etc. If you want to read a bit more about bytecode itself and instructions then there is a great documentation of all JVM instructions here: https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-6.html.

In this section, we will cover some basics about java bytecode.

In bytecode, there is a concept called "the stack". You might remember it from an exception called `StackOverflowError` or from the website `stackoverflow.com`. We will dig into what is exactly stack, and how bytecode uses it.

Consider this java code example:
```java
public static void main(String[] args) {
    System.out.println("Hello World!");
}
```
This is its bytecode:
```shell
getstatic java/lang/System.out Ljava/io/PrintStream;
ldc "Hello World!"
invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V
```
Let's break down these instructions.
```shell
getstatic java/lang/System.out Ljava/io/PrintStream; # Stack: (System.out)
ldc "Hello World!" # Stack: (System.out, "Hello World!")
# Stack operations:
# 1. Pop the top value - The argument for "println" method 
# Stack before: (System.out, "Hello World!")
# Stack after: (System.out)
# 2. Pop the top value - The object the method is invoked from
# Stack before: (System.out)
# Stack after: ()
invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V
```
The name of the instruction is called an `opcode`. Here are the opcodes used in the example:
- `getstatic` - Gets the value of a static field.
- `ldc` - Loads a constant value onto the stack.
- `invokevirtual` - Invokes a method on an object.

Here you can see that the stack is used to pass arguments to methods and to store the object the method is invoked from. The stack is also used to store the return value of the method.

Let's now break down the syntax of these instructions:
```shell
getstatic (class name).(field name) (field descriptor)
ldc (any constant value)
invokevirtual (class name).(method name) (method descriptor)
```
The class name, field name and method name are self-explanatory. But what are these `field descriptor` and `method descriptor`?
- Field descriptor - Describes the type of the field. For example, `Ljava/lang/String;` is the descriptor for the `String` class. Equivalent to `public String someName;`
- Method descriptor - Describes the method signature (argument types and return type). For example, `(ILjava/lang/String;)Z` means that method takes `int` as a first argument, `String` as a second argument and it returns `boolean`. Equivalent to `public boolean someName(int arg1, String arg2)`.

Here you can find the list of all descriptors: https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3. You need to scroll down a bit to find the table with descriptors. If you can't find it then hit <kbd>CTRL</kbd>+<kbd>F</kbd> and search for `Table 4.3-A. Interpretation of field descriptors`.

## 🪄 Transformers
### What are transformers?
The whole deobfuscation process is based on transformers.
Transformers are smaller pieces that are responsible for deobfuscating specific obfuscation techniques.
In simple words - transformers are transforming obfuscated code into a more readable form.

### How to create your own transformer?
Create a new class in [`deobfuscator-transformers`](./deobfuscator-transformers) module. The most basic transformer can look like this:
```java
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class SomeTransformer extends Transformer {
  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      // Code here
    }));
  }
}
```
For example, this transformer replaces every "Hello, World!" string with "Bye, World!":
```java
import org.objectweb.asm.tree.LdcInsnNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Arrays;

public class SomeTransformer extends Transformer {
  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      
      // Iterate over all LDC instructions in the method
      Arrays.stream(methodNode.instructions.toArray())
          .filter(insn -> insn.getOpcode() == LDC) // Check if opcode is LDC
          .map(LdcInsnNode.class::cast) // Cast to LdcInsnNode so we can access the constant value (ldcInsn.cst)
          .forEach(ldcInsn -> {
            // Replace all "Hello, World!" strings with "Bye, World!"
            if (ldcInsn.cst.equals("Hello, World!")) {
              ldcInsn.cst = "Bye, World!";
            }
          });
    }));
  }
}
```
You can also get stack values that are pushed before the instruction. For example, if you want to replace all strings with "Bye, World!" only in `System.out.println` calls:
```java
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.asm.InstructionContext;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.Arrays;

public class SomeTransformer extends Transformer {
  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).forEach(classWrapper -> classWrapper.methods().forEach(methodNode -> {
      MethodContext methodContext = MethodContext.framed(classWrapper, methodNode);

      // Find all System.out.println calls and replace the string with "Bye, World!"
      Arrays.stream(methodNode.instructions.toArray())
          .filter(insn -> insn.getOpcode() == INVOKEVIRTUAL) // Match only INVOKEVIRTUAL instructions
          .forEach(insn -> {
            MethodInsnNode methodInsn = (MethodInsnNode) insn;

            // Find System.out.println call
            if (methodInsn.owner.equals("java/io/PrintStream") && methodInsn.name.equals("println") && methodInsn.desc.equals("(Ljava/lang/String;)V")) {
              // Create instruction context. Required for getting stack values.
              InstructionContext insnContext = methodContext.newInsnContext(methodInsn);
              Frame<OriginalSourceValue> frame = insnContext.frame();

              // Get top value from the stack
              OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);

              // Remove all instructions that produced the top stack value. We will replace them with our own instruction.
              for (AbstractInsnNode producer : sourceValue.insns) {
                methodNode.instructions.remove(producer);
              }

              // Replace the top stack value with the string "Bye, World!"
              methodNode.instructions.insertBefore(methodInsn, new LdcInsnNode("Bye, World!"));
            }
          });
    }));
  }
}
```
The same effect you can achieve by using a utility class called `FramedInstructionsStream`. In this way, we are minimizing the boilerplate code: 
```java
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.helper.FramedInstructionsStream;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class SomeTransformer extends Transformer {
  @Override
  protected void transform(ClassWrapper scope, Context context) throws Exception {
    FramedInstructionsStream.of(scope, context)
        .editInstructionsStream(stream -> stream.filter(insn -> insn.getOpcode() == INVOKEVIRTUAL)) // Match only INVOKEVIRTUAL instructions
        .forEach(insnContext -> {
          MethodInsnNode methodInsn = (MethodInsnNode) insnContext.insn();

          // Find System.out.println call
          if (methodInsn.owner.equals("java/io/PrintStream") && methodInsn.name.equals("println") && methodInsn.desc.equals("(Ljava/lang/String;)V")) {
            Frame<OriginalSourceValue> frame = insnContext.frame();

            // Get top value from the stack
            OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);

            // Remove all instructions that produced the top stack value. We will replace them with our own instruction.
            for (AbstractInsnNode producer : sourceValue.insns) {
              insnContext.methodNode().instructions.remove(producer);
            }

            // Replace the top stack value with the string "Bye, World!"
            insnContext.methodNode().instructions.insertBefore(methodInsn, new LdcInsnNode("Bye, World!"));
          }
        });
  }
}
```
But why do we need to get a stack value from frame? Can't we just move one instruction up and replace it? The answer is: not always. Sometimes the value is produced somewhere else, so the stack value may be much further produced. To see this issue in action, consider this example:
```shell
ldc "Hello World!"  # Stack: ("Hello World!")
dup # Stack: ("Hello World!", "Hello World!")
dup # Stack: ("Hello World!", "Hello World!", "Hello World!")
getstatic java/lang/System.out Ljava/io/PrintStream;  # Stack: ("Hello World!", "Hello World!", "Hello World!", System.out)
swap # Stack: ("Hello World!", "Hello World!", System.out, "Hello World!")
invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V # Stack: ("Hello World!", "Hello World!")
pop # Stack: ("Hello World!")
pop # Stack: ()
```
The above example shows that the value is produced by `ldc` instruction, but it is used much later. So this is still valid JVM bytecode AND `ldc "Hello World!"` is not straight before `invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V`. This is one of a very common obfuscation techniques. Fortunately, there already exists a universal transformer that removes these useless DUP and POP pairs ([UselessPopCleanTransformer](./deobfuscator-transformers/src/main/java/uwu/narumi/deobfuscator/core/other/impl/clean/peephole/UselessPopCleanTransformer.java)) but only simple forms of them.

The project is also greatly documented, so you can find more information about any class or transformer in their javadocs and comments inside the code. You can also view other transformers' code - see how they work and how they are implemented. There are also tests, so you can, for example, see how the transformer behaves when you modify some line of code.

## 🧪 Testing
### How do these tests work?
The deobfuscator has a clever testing system. It works as follows:
1. The [registered samples](./deobfuscator-impl/src/test/java/uwu/narumi/deobfuscator/TestDeobfuscation.java) are transformed using corresponding transformers.
2. The output gets decompiled using Vineflower.
3. The decompiled code is compared against the [expected output](./testData/results).

### How to run tests?
Run command `mvn test` in the root directory of the project.

### How to create your own tests?
You can create your own tests for transformers. There are a few ways to do it:
- If the obfuscation is simple enough, you can write your own sample in [`testData/src/java`](./testData/src/java)
- If the obfuscation is more complex, you can throw your raw obfuscated classes (`.class` files) into [`testData/compiled/custom-classes`](./testData/compiled/custom-classes) and test transformers on them.
- You can also throw your obfuscated jars into [`testData/compiled/custom-jars`](./testData/compiled/custom-jars) and test transformers on them.

You also need to register each sample in class [TestDeobfuscation.java](./deobfuscator-impl/src/test/java/uwu/narumi/deobfuscator/TestDeobfuscation.java)

