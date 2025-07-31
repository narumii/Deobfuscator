package uwu.narumi.deobfuscator.core.other.impl.clean;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.InsnContext;
import uwu.narumi.deobfuscator.api.asm.MethodContext;
import uwu.narumi.deobfuscator.api.asm.MethodRef;
import uwu.narumi.deobfuscator.api.helper.AsmHelper;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

import java.util.*;

public class InlineParameters extends Transformer {
  public static final Map<MethodRef,Map<Integer,Object>> parameters = new HashMap<>();
  //<method,<index,value>>

  @Override
  protected void transform() throws Exception {
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(method -> {

      MethodContext mc = MethodContext.of(classWrapper,method);
      method.instructions.forEach(insn -> {
        InsnContext ic = mc.at(insn);

        if(insn instanceof MethodInsnNode min){
          MethodRef mr = MethodRef.of(min);
          Frame<OriginalSourceValue> frame= ic.frame();

          int paramCount = Type.getArgumentTypes(min.desc).length;


          for(int i =1;i <= paramCount;i++){
            int offset = min.getOpcode() == Opcodes.INVOKESTATIC ? 1 : 0;

            assert frame != null;
            OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - (i));

            if(sourceValue == null)continue;

            if(sourceValue.getConstantValue() != null){

              if(!parameters.containsKey(mr))parameters.put(mr,new HashMap<>());

              Object value = sourceValue.getConstantValue().value();

              int var = paramCount - (i + offset) + 1;

              //被内联的常量必须保证全部相同
              if(parameters.get(mr).containsKey( var ) &&
                  (parameters.get(mr).get( var ) == null || !parameters.get(mr).get( var ).equals( value ))){
                parameters.get(mr).put(var, null);
              }
              else parameters.get(mr).put(var, value);
            }
          }
        }
      });
    }));
    //System.out.println(parameters);
    scopedClasses().forEach(classWrapper -> classWrapper.methods().forEach(method -> {
      MethodRef mr = MethodRef.of(classWrapper.classNode(),method);
      //if(!mr.name().equals("<init>"))return;
      if(!parameters.containsKey(mr))return;

      Map<Integer,Object> map = fixLongValueMap(parameters.get(mr),method.desc,(method.access & Opcodes.ACC_STATIC) != 0);

      System.out.println(mr+" "+parameters.get(mr) + " "+((method.access & Opcodes.ACC_STATIC) != 0));
      System.out.println(mr+" "+map + " "+((method.access & Opcodes.ACC_STATIC) != 0));


      Arrays.stream(method.instructions.toArray())
          .filter((insn) -> insn instanceof VarInsnNode && insn.isVarLoad())
          .map((insn) -> (VarInsnNode) insn)
          .forEach(varInsn -> {
            if(map.containsKey(varInsn.var) && map.get(varInsn.var) != null){
              AbstractInsnNode constant = AsmHelper.toConstantInsn(map.get(varInsn.var));
              if(constant.isConstant()){// always true:xD
                method.instructions.set(varInsn, constant);
                map.put(varInsn.var, null);// only use once
                markChange();
              }

            }
          })
      ;
    }));

  }
  public static Map<Integer, Object> fixLongValueMap(Map<Integer, Object> map, String description, boolean isStatic) {
    Map<Integer, Object> fixedMap = new HashMap<>();
    Type[] argTypes = Type.getArgumentTypes(description);
    int offset = isStatic ? 0 : 1;
    // 参数索引到局部变量表槽位的映射
    int localVarIndex = offset; // 非静态方法跳过this

    for (int paramIndex = 0; paramIndex < argTypes.length; paramIndex++) {
      Type argType = argTypes[paramIndex];
      int paramSize = argType.getSize();

      if (map.containsKey(paramIndex + offset)) {
        Object value = map.get(paramIndex + offset);

        // 处理long/double类型
        if (paramSize == 2) {
          fixedMap.put(localVarIndex, value);
          // 标记高位槽位为已处理（通常不需要实际值）
          fixedMap.put(localVarIndex + 1, null);
        } else {
          fixedMap.put(localVarIndex, value);
        }
      }

      localVarIndex += paramSize;
    }

    return fixedMap;
  }
}
