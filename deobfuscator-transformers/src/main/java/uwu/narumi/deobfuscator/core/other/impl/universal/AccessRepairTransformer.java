package uwu.narumi.deobfuscator.core.other.impl.universal;

import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class AccessRepairTransformer extends Transformer {

  private final int[] CLASS = {
      ACC_STATIC,
      ACC_SYNCHRONIZED,
      ACC_OPEN,
      ACC_TRANSITIVE,
      ACC_VOLATILE,
      ACC_BRIDGE,
      ACC_STATIC_PHASE,
      ACC_VARARGS,
      ACC_TRANSIENT,
      ACC_NATIVE,
      ACC_STRICT,
      ACC_SYNTHETIC
  };

  private final int[] METHOD = {
      ACC_SUPER,
      ACC_OPEN,
      ACC_TRANSITIVE,
      ACC_VOLATILE,
      ACC_STATIC_PHASE,
      ACC_TRANSIENT,
      ACC_INTERFACE,
      ACC_ANNOTATION,
      ACC_ENUM,
      ACC_MODULE,
      ACC_RECORD,
      ACC_SYNTHETIC,
      ACC_BRIDGE
  };

  private final int[] FIELD = {
      ACC_SUPER,
      ACC_SYNCHRONIZED,
      ACC_OPEN,
      ACC_TRANSITIVE,
      ACC_BRIDGE,
      ACC_STATIC_PHASE,
      ACC_VARARGS,
      ACC_NATIVE,
      ACC_INTERFACE,
      ACC_ABSTRACT,
      ACC_STRICT,
      ACC_ANNOTATION,
      ACC_MODULE,
      ACC_RECORD,
      ACC_SYNTHETIC
  };

  private final int[] PARAMETER = {
      ACC_PUBLIC,
      ACC_PRIVATE,
      ACC_PROTECTED,
      ACC_STATIC,
      ACC_SUPER,
      ACC_SYNCHRONIZED,
      ACC_OPEN,
      ACC_TRANSITIVE,
      ACC_VOLATILE,
      ACC_BRIDGE,
      ACC_STATIC_PHASE,
      ACC_VARARGS,
      ACC_TRANSIENT,
      ACC_NATIVE,
      ACC_INTERFACE,
      ACC_ABSTRACT,
      ACC_STRICT,
      ACC_ANNOTATION,
      ACC_ENUM,
      ACC_MODULE,
      ACC_RECORD,
      ACC_DEPRECATED,
      ACC_SYNTHETIC
  };

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> {
      int classAccess = classWrapper.classNode().access;
      for (int access : CLASS) {
        if (isAccess(classAccess, access)) {
          classAccess &= ~access;
          this.markChange();
        }
      }
      classWrapper.classNode().access = classAccess;

      classWrapper.methods().forEach(methodNode -> {
        for (int access : METHOD) {
          if (isAccess(methodNode.access, access)) {
            methodNode.access &= ~access;
            this.markChange();
          }
        }

        if (methodNode.parameters != null)
          methodNode.parameters.forEach(parameterNode -> {
            for (int access : PARAMETER) {
              if (isAccess(parameterNode.access, access)) {
                parameterNode.access &= ~access;
                this.markChange();
              }
            }
          });
      });

      classWrapper.fields().forEach(fieldNode -> {
        for (int access : FIELD) {
          if (isAccess(fieldNode.access, access)) {
            fieldNode.access &= ~access;
            this.markChange();
          }
        }
      });

      // TODO: Module maybe?
    });
  }
}
