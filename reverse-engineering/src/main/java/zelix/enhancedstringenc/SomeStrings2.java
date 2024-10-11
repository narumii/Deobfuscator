package zelix.enhancedstringenc;

/**
 * This is just a very similar class to {@link SomeStrings} but with different strings. It exists here to compare two classes.
 */
public class SomeStrings2 {
  private static final String[] a;
  private static final String[] b;

  public static void main(String[] var0) {
    System.out.println(a(27040, -3854));
    System.out.println(a(27041, 8907));
  }

  // $VF: Irreducible bytecode was duplicated to produce valid code
  static {
    String[] var5 = new String[2];
    int var3 = 0;
    String var2 = "º\u009bæ½W\u0006x%\\!ÖI";
    int var4 = "º\u009bæ½W\u0006x%\\!ÖI".length();
    char var1 = 5;
    int var0 = -1;

    while (true) {
      char[] var16;
      label38: {
        char[] var10001 = var2.substring(++var0, var0 + var1).toCharArray();
        int var10003 = var10001.length;
        int var6 = 0;
        var16 = var10001;
        int var10 = var10003;
        char[] var23;
        int var10004;
        if (var10003 <= 1) {
          var23 = var10001;
          var10004 = var6;
        } else {
          var16 = var10001;
          var10 = var10003;
          if (var10003 <= var6) {
            break label38;
          }

          var23 = var10001;
          var10004 = var6;
        }

        while (true) {
          var23[var10004] = (char)(var23[var10004] ^ switch (var6 % 7) {
            case 0 -> 37;
            case 1 -> 43;
            case 2 -> 56;
            case 3 -> 14;
            case 4 -> 2;
            case 5 -> 108;
            default -> 14;
          });
          var6++;
          if (var10 == 0) {
            var10004 = var10;
            var23 = var16;
          } else {
            if (var10 <= var6) {
              break;
            }

            var23 = var16;
            var10004 = var6;
          }
        }
      }

      String var30 = new String(var16).intern();
      byte var14 = -1;
      var5[var3++] = var30;
      if ((var0 += var1) >= var4) {
        a = var5;
        b = new String[2];
        break;
      }

      var1 = var2.charAt(var0);
    }
  }

  private static String a(int var0, int var1) {
    int var2 = (var0 ^ 27040) & 65535;
    if (b[var2] == null) {
      char[] var3 = a[var2].toCharArray();

      short var4 = switch (var3[0] & 0xFF) {
        case 0 -> 101;
        case 1 -> 243;
        case 2 -> 232;
        case 3 -> 26;
        case 4 -> 83;
        case 5 -> 33;
        case 6 -> 106;
        case 7 -> 211;
        case 8 -> 217;
        case 9 -> 10;
        case 10 -> 113;
        case 11 -> 188;
        case 12 -> 52;
        case 13 -> 25;
        case 14 -> 87;
        case 15 -> 196;
        case 16 -> 182;
        case 17 -> 15;
        case 18 -> 191;
        case 19 -> 93;
        case 20 -> 143;
        case 21 -> 112;
        case 22 -> 71;
        case 23 -> 183;
        case 24 -> 35;
        case 25 -> 205;
        case 26 -> 178;
        case 27 -> 138;
        case 28 -> 224;
        case 29 -> 173;
        case 30 -> 126;
        case 31 -> 132;
        case 32 -> 17;
        case 33 -> 46;
        case 34 -> 47;
        case 35 -> 189;
        case 36 -> 19;
        case 37 -> 45;
        case 38 -> 97;
        case 39 -> 120;
        case 40 -> 69;
        case 41 -> 63;
        case 42 -> 20;
        case 43 -> 167;
        case 44 -> 92;
        case 45 -> 104;
        case 46 -> 61;
        case 47 -> 117;
        case 48 -> 213;
        case 49 -> 230;
        case 50 -> 212;
        case 51 -> 174;
        case 52 -> 255;
        case 53 -> 107;
        case 54 -> 13;
        case 55 -> 170;
        case 56 -> 242;
        case 57 -> 109;
        case 58 -> 80;
        case 59 -> 198;
        case 60 -> 12;
        case 61 -> 246;
        case 62 -> 114;
        case 63 -> 180;
        case 64 -> 252;
        case 65 -> 94;
        case 66 -> 184;
        case 67 -> 30;
        case 68 -> 89;
        case 69 -> 192;
        case 70 -> 201;
        case 71 -> 73;
        case 72 -> 135;
        case 73 -> 238;
        case 74 -> 203;
        case 75 -> 66;
        case 76 -> 78;
        case 77 -> 239;
        case 78 -> 37;
        case 79 -> 137;
        case 80 -> 228;
        case 81 -> 153;
        case 82 -> 234;
        case 83 -> 160;
        case 84 -> 77;
        case 85 -> 240;
        case 86 -> 58;
        case 87 -> 5;
        case 88 -> 187;
        case 89 -> 133;
        case 90 -> 115;
        case 91 -> 145;
        case 92 -> 65;
        case 93 -> 193;
        case 94 -> 172;
        case 95 -> 241;
        case 96 -> 1;
        case 97 -> 144;
        case 98 -> 175;
        case 99 -> 186;
        case 100 -> 185;
        case 101 -> 200;
        case 102 -> 194;
        case 103 -> 208;
        case 104 -> 219;
        case 105 -> 171;
        case 106 -> 72;
        case 107 -> 152;
        case 108 -> 123;
        case 109 -> 122;
        case 110 -> 39;
        case 111 -> 149;
        case 112 -> 56;
        case 113 -> 90;
        case 114 -> 79;
        case 115 -> 134;
        case 116 -> 195;
        case 117 -> 141;
        case 118 -> 197;
        case 119 -> 74;
        case 120 -> 28;
        case 121 -> 210;
        case 122 -> 158;
        case 123 -> 164;
        case 124 -> 150;
        case 125 -> 105;
        case 126 -> 250;
        case 127 -> 3;
        case 128 -> 146;
        case 129 -> 24;
        case 130 -> 225;
        case 131 -> 127;
        case 132 -> 131;
        case 133 -> 177;
        case 134 -> 68;
        case 135 -> 75;
        case 136 -> 128;
        case 137 -> 202;
        case 138 -> 236;
        case 139 -> 165;
        case 140 -> 95;
        case 141 -> 156;
        case 142 -> 18;
        case 143 -> 116;
        case 144 -> 84;
        case 145 -> 155;
        case 146 -> 22;
        case 147 -> 29;
        case 148 -> 179;
        case 149 -> 0;
        case 150 -> 103;
        case 151 -> 43;
        case 152 -> 99;
        case 153 -> 204;
        case 154 -> 229;
        case 155 -> 54;
        case 156 -> 9;
        case 157 -> 130;
        case 158 -> 32;
        case 159 -> 27;
        case 160 -> 162;
        case 161 -> 119;
        case 162 -> 31;
        case 163 -> 38;
        case 164 -> 118;
        case 165 -> 36;
        case 166 -> 48;
        case 167 -> 4;
        case 168 -> 235;
        case 169 -> 253;
        case 170 -> 67;
        case 171 -> 86;
        case 172 -> 44;
        case 173 -> 216;
        case 174 -> 139;
        case 175 -> 209;
        case 176 -> 154;
        case 177 -> 166;
        case 178 -> 82;
        case 179 -> 129;
        case 180 -> 233;
        case 181 -> 140;
        case 182 -> 108;
        case 183 -> 59;
        case 184 -> 163;
        case 185 -> 96;
        case 186 -> 226;
        case 187 -> 53;
        case 188 -> 2;
        case 189 -> 227;
        case 190 -> 223;
        case 191 -> 111;
        case 192 -> 124;
        case 193 -> 6;
        case 194 -> 215;
        case 195 -> 40;
        case 196 -> 142;
        case 197 -> 231;
        case 198 -> 60;
        case 199 -> 181;
        case 200 -> 102;
        case 201 -> 206;
        case 202 -> 125;
        case 203 -> 214;
        case 204 -> 251;
        case 205 -> 49;
        case 206 -> 249;
        case 207 -> 16;
        case 208 -> 23;
        case 209 -> 207;
        case 210 -> 237;
        case 211 -> 62;
        case 212 -> 34;
        case 213 -> 169;
        case 214 -> 218;
        case 215 -> 21;
        case 216 -> 148;
        case 217 -> 159;
        case 218 -> 81;
        case 219 -> 168;
        case 220 -> 248;
        case 221 -> 151;
        case 222 -> 11;
        case 223 -> 147;
        case 224 -> 7;
        case 225 -> 221;
        case 226 -> 254;
        case 227 -> 136;
        case 228 -> 100;
        case 229 -> 98;
        case 230 -> 14;
        case 231 -> 247;
        case 232 -> 161;
        case 233 -> 85;
        case 234 -> 220;
        case 235 -> 50;
        case 236 -> 70;
        case 237 -> 244;
        case 238 -> 91;
        case 239 -> 55;
        case 240 -> 176;
        case 241 -> 199;
        case 242 -> 245;
        case 243 -> 64;
        case 244 -> 121;
        case 245 -> 76;
        case 246 -> 8;
        case 247 -> 51;
        case 248 -> 42;
        case 249 -> 57;
        case 250 -> 88;
        case 251 -> 222;
        case 252 -> 190;
        case 253 -> 157;
        case 254 -> 41;
        default -> 110;
      };
      int var5 = (var1 & 0xFF) - var4;
      if (var5 < 0) {
        var5 += 256;
      }

      int var6 = ((var1 & 65535) >>> 8) - var4;
      if (var6 < 0) {
        var6 += 256;
      }

      for (int var7 = 0; var7 < var3.length; var7++) {
        int var8 = var7 % 2;
        char var10002 = var3[var7];
        if (var8 == 0) {
          var3[var7] = (char)(var10002 ^ var5);
          var5 = ((var5 >>> 3 | var5 << 5) ^ var3[var7]) & 0xFF;
        } else {
          var3[var7] = (char)(var10002 ^ var6);
          var6 = ((var6 >>> 3 | var6 << 5) ^ var3[var7]) & 0xFF;
        }
      }

      b[var2] = new String(var3).intern();
    }

    return b[var2];
  }
}
