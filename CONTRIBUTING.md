# Contributing

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
  - `deobfuscated` - Raw classes after deobfuscation process. Useful when debugging.
  - [`results`](./testData/results) - Expected results that are auto-generated decompiled java code.
- [`TestDeobfuscation.java`](./deobfuscator-impl/src/test/java/uwu/narumi/deobfuscator/TestDeobfuscation.java) - Class where each test sample is registered.
- [`Bootstrap.java`](./deobfuscator-impl/src/test/java/Bootstrap.java) - Class where you can run deobfuscator manually.

## 🧰 Recommended tools
- [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) - IDE for Java development
- [Recaf](https://github.com/Col-E/Recaf) - Modern java bytecode editor. Use it to analyze obfuscated classes.

## 🪄 Transformers
### What are transformers?
Whole deobfuscation process is based on transformers. Transformers are smaller pieces that are responsible for deobfuscating specific obfuscation techniques. In simple words - transformers are transforming obfuscated code into a more readable form.

### How to create your own transformer?
1. Create a new class in [`deobfuscator-transformers`](./deobfuscator-transformers) module.
2. Pick `Transformer`-like class you would like to implement:
   - `Transformer` - Basic transformer that transforms classes.
   - `ComposedTransformer` - Transformer that consists of multiple transformers.
3. You can start coding!

## 🧪 Testing
### How these tests work?
1. The [registered samples](./deobfuscator-impl/src/test/java/uwu/narumi/deobfuscator/TestDeobfuscation.java) are transformed using corresponding transformers.
2. The output gets decompiled using Vineflower.
3. The decompiled code is compared against the [expected output](./testData/results).

### How to run tests?
Just run command `mvn test` in the root directory of the project.

### How to create your own tests?
You can create your own tests for transformers. There are a few ways to do it:
- If the obfuscation is simple enough, you can write your own sample in [`testData/src/java`](./testData/src/java)
- If the obfuscation is more complex, you can throw your raw obfuscated classes (`.class` files) into [`testData/compiled/custom-classes`](./testData/compiled/custom-classes) and test transformers on them.
- You can also throw your obfuscated jars into [`testData/compiled/custom-jars`](./testData/compiled/custom-jars) and test transformers on them.

You also need to register each sample in class [TestDeobfuscation.java](./deobfuscator-impl/src/test/java/uwu/narumi/deobfuscator/TestDeobfuscation.java)

