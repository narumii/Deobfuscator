# Diobfuscator
A deobfuscator for java

> If you are looking for v1 version of Diobfuscator, you can find it [here](https://github.com/narumii/Deobfuscator/tree/v1)

## ✅ How to run deobfuscator
If you want to use this deobfuscator, you need to start it from your IDE manually.

1. Clone this repository and open it in IntelliJ
2. (Windows) Open cmd as administrator and run `git config --system core.longpaths true` to enable long paths support
3. Make sure that you have selected [Java 17 (Temurin)](https://adoptium.net/temurin/releases/?version=17) in `Project Structure` -> `SDK`
4. Place your obfuscated jar inside the root project directory. For example in `work/obf-test.jar`
5. Navigate to class [`Bootstrap.java`](./deobfuscator-impl/src/test/java/Bootstrap.java)
6. In this class edit the deobfuscator configuration
    - `inputJar` - Your obfuscated jar file that you placed in step 1
    - `transformers` - Pick transformers that you want to run. You can find them in [`deobfuscator-transformers`](./deobfuscator-transformers/src/main/java/uwu/narumi/deobfuscator/core/other) module.
7. Run this class manually from your IDE. You can use our pre-configured IntelliJ task named `Bootstrap`.

![tak](./assets/run-deobfuscator.gif)

## 🔧 Contributing
Contributions are welcome! See [CONTRIBUTING.md](./CONTRIBUTING.md) for a project introduction and some basics about java bytecode.

## Troubleshooting

### Git shows `Filename too long`

You need to open cmd as administrator and run `git config --system core.longpaths true`. This error is only shown for windows users.

![img.png](assets/filename-too-long.png)

## Links

<a href="https://discord.gg/tRU27KtPAZ"><img src="https://discordapp.com/api/guilds/900083350314811432/widget.png?style=banner2"/></a>

