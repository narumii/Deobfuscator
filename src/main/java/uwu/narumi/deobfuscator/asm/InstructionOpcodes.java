package uwu.narumi.deobfuscator.asm;

/*
    Idk what i do here just ignore this shit pls my brain doesn't work
 */
public interface InstructionOpcodes {

    int pop = 87;
    int pop2 = 88;
    int dup = 89;
    int dup_x1 = 90;
    int dup_x2 = 91;
    int dup2 = 92;
    int dup2_x1 = 93;
    int dup2_x2 = 94;
    int swap = 95;

    int iadd = 96;
    int isub = 100;
    int imul = 104;
    int irem = 112;
    int ineg = 116;
    int ishl = 120;
    int iand = 126;
    int ior = 128;
    int ixor = 130;

    int iconst_m1 = 2;
    int iconst_0 = 3;
    int iconst_1 = 4;
    int iconst_2 = 5;
    int iconst_3 = 6;
    int iconst_4 = 7;
    int iconst_5 = 8;
}
