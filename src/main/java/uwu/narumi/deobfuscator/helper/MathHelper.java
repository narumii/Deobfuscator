package uwu.narumi.deobfuscator.helper;

import org.objectweb.asm.Opcodes;

import java.util.regex.Pattern;

/*
    TODO: Fix boilerplate
 */
public class MathHelper implements Opcodes {

    public static final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d+$");

    public static Integer doMath(int opcode, int first, int second) {
        switch (opcode) {
            case IADD:
                return first + second;
            case ISUB:
                return first - second;
            case IMUL:
                return first * second;
            case IDIV:
                return first / opcode;
            case IREM:
                return first % second;
            case ISHL:
                return first << second;
            case ISHR:
                return first >> second;
            case IUSHR:
                return first >>> second;
            case IAND:
                return first & second;
            case IOR:
                return first | second;
            case IXOR:
                return first ^ second;
            case INEG: //Idk why i put it here
                return -first;
            default:
                return null;
        }
    }

    public static Long doMath(int opcode, long first, long second) {
        switch (opcode) {
            case LADD:
                return first + second;
            case LSUB:
                return first - second;
            case LMUL:
                return first * second;
            case LDIV:
                return first / opcode;
            case LREM:
                return first % second;
            case LSHL:
                return first << second;
            case LSHR:
                return first >> second;
            case LUSHR:
                return first >>> second;
            case LAND:
                return first & second;
            case LOR:
                return first | second;
            case LXOR:
                return first ^ second;
            case LNEG:
                return -first; //Idk why i put it here
            default:
                return null;
        }
    }
}
