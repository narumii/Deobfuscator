package zelix.enhancedstringenc;

public class SomeStrings {
  private static final String[] staticObfuscatedStrings;
  private static final String[] stringsCache;

  public static void main(String[] var0) {
    System.out.println(decryptString(342, -8945)); // placek
    System.out.println(decryptString(341, -24830)); // tak
    System.out.println(decryptString(340, 25523)); // Wrong Password
    //a(decryptString(342, -8945));
  }

  private static void a(String var0) {
    if (!var0.equals(decryptString(341, -24830))) {
      throw new RuntimeException(decryptString(340, 25523));
    }
  }

  // $VF: Irreducible bytecode was duplicated to produce valid code
  // Cleaned up redundant code
  static {
    String[] decodedStrings = new String[3];
    int decodedIndex = 0;
    // This long string contains all obfuscated strings
    String obfuscatedStrings = "\n´jjx\u001ffÝT|N-óñ\u0003Ó\u000e\u0012\u0006\u0015¬D¹p\u0082"; // Dynamic
    int obfuscatedLength = "\n´jjx\u001ffÝT|N-óñ\u0003Ó\u000e\u0012\u0006\u0015¬D¹p\u0082".length(); // Dynamic
    char substringLength = 14; // Dynamic
    int currentIndex = -1;

    while (true) {
      String substring = obfuscatedStrings.substring(++currentIndex, currentIndex + substringLength);
      //System.out.println("current index: "+currentIndex);
      //System.out.println("substring length: "+(int)substringLength);
      char[] charArray = substring.toCharArray();
      //System.out.println(Arrays.toString(charArray));
      /*label38:*/ //{
        //charArray
        //int arrayLength = charArray.length;
        //int charIndex = 0;
        //charArray = tempCharArray;
        //char[] tempArray;
        //int tempIndex;
        /*if (arrayLength <= 1) {
          tempArray = tempCharArray;
          tempIndex = charIndex;
        } else {
          charArray = tempCharArray;
          tempArrayLength = arrayLength;
          if (arrayLength <= charIndex) {
            break label38;
          }*/

          //tempArray = tempCharArray;
          //tempIndex = charIndex;
        //}

      // XOR char array
      for (int i = 0; i < charArray.length; i++) {
        // Dynamic start
        charArray[i] = (char) (charArray[i] ^ switch (i % 7) {
          case 0 -> 116;
          case 1 -> 31;
          case 2 -> 119;
          case 3 -> 77;
          case 4 -> 62;
          case 5 -> 120;
          default -> 117;
        });
        // Dynamic end
        //charIndex++;
      }

          //tempArray = charArray;
          //tempIndex = charIndex;

      //}

      String decodedString = new String(charArray).intern();
      //System.out.println(decodedString);
      //byte var14 = -1;
      decodedStrings[decodedIndex++] = decodedString;
      if ((currentIndex += substringLength) >= obfuscatedLength) {
        break; // return;
      }

      substringLength = obfuscatedStrings.charAt(currentIndex);
    }
    staticObfuscatedStrings = decodedStrings;
    stringsCache = new String[3];

    // Entrypoint (clinit execution)
  }

  private static String decryptString(int key1, int key2) {
    // 340 - Dynamic mask
    int index = (key1 ^ 340) & 65535;
    //System.out.println(index);

    if (stringsCache[index] == null) {
      char[] obfuscatedChars = staticObfuscatedStrings[index].toCharArray();

      // Dynamic start
      // Determine the initial key based on the first character of the obfuscated string
      short initialKey = switch (obfuscatedChars[0] & 0xFF) {
        case 0 -> 242;
        case 1 -> 171;
        case 2 -> 94;
        case 3 -> 179;
        case 4 -> 15;
        case 5 -> 170;
        case 6 -> 103;
        case 7 -> 245;
        case 8 -> 176;
        case 9 -> 173;
        case 10 -> 80;
        case 11 -> 95;
        case 12 -> 249;
        case 13 -> 190;
        case 14 -> 81;
        case 15 -> 231;
        case 16 -> 104;
        case 17 -> 197;
        case 18 -> 168;
        case 19 -> 125;
        case 20 -> 172;
        case 21 -> 35;
        case 22 -> 131;
        case 23 -> 4;
        case 24 -> 91;
        case 25 -> 3;
        case 26 -> 203;
        case 27 -> 237;
        case 28 -> 2;
        case 29 -> 20;
        case 30 -> 18;
        case 31 -> 130;
        case 32 -> 89;
        case 33 -> 65;
        case 34 -> 25;
        case 35 -> 19;
        case 36 -> 30;
        case 37 -> 127;
        case 38 -> 68;
        case 39 -> 182;
        case 40 -> 152;
        case 41 -> 225;
        case 42 -> 7;
        case 43 -> 189;
        case 44 -> 191;
        case 45 -> 139;
        case 46 -> 14;
        case 47 -> 240;
        case 48 -> 207;
        case 49 -> 110;
        case 50 -> 6;
        case 51 -> 210;
        case 52 -> 159;
        case 53 -> 185;
        case 54 -> 48;
        case 55 -> 105;
        case 56 -> 236;
        case 57 -> 144;
        case 58 -> 77;
        case 59 -> 49;
        case 60 -> 8;
        case 61 -> 165;
        case 62 -> 108;
        case 63 -> 150;
        case 64 -> 187;
        case 65 -> 157;
        case 66 -> 143;
        case 67 -> 86;
        case 68 -> 78;
        case 69 -> 116;
        case 70 -> 118;
        case 71 -> 193;
        case 72 -> 164;
        case 73 -> 166;
        case 74 -> 9;
        case 75 -> 43;
        case 76 -> 59;
        case 77 -> 75;
        case 78 -> 251;
        case 79 -> 137;
        case 80 -> 38;
        case 81 -> 60;
        case 82 -> 63;
        case 83 -> 11;
        case 84 -> 149;
        case 85 -> 220;
        case 86 -> 31;
        case 87 -> 167;
        case 88 -> 98;
        case 89 -> 113;
        case 90 -> 109;
        case 91 -> 232;
        case 92 -> 204;
        case 93 -> 51;
        case 94 -> 129;
        case 95 -> 194;
        case 96 -> 45;
        case 97 -> 254;
        case 98 -> 134;
        case 99 -> 153;
        case 100 -> 122;
        case 101 -> 184;
        case 102 -> 228;
        case 103 -> 243;
        case 104 -> 227;
        case 105 -> 23;
        case 106 -> 74;
        case 107 -> 135;
        case 108 -> 120;
        case 109 -> 252;
        case 110 -> 148;
        case 111 -> 71;
        case 112 -> 124;
        case 113 -> 39;
        case 114 -> 205;
        case 115 -> 136;
        case 116 -> 132;
        case 117 -> 112;
        case 118 -> 223;
        case 119 -> 57;
        case 120 -> 58;
        case 121 -> 10;
        case 122 -> 253;
        case 123 -> 235;
        case 124 -> 111;
        case 125 -> 158;
        case 126 -> 138;
        case 127 -> 209;
        case 128 -> 222;
        case 129 -> 140;
        case 130 -> 34;
        case 131 -> 53;
        case 132 -> 229;
        case 133 -> 208;
        case 134 -> 177;
        case 135 -> 83;
        case 136 -> 5;
        case 137 -> 27;
        case 138 -> 73;
        case 139 -> 56;
        case 140 -> 255;
        case 141 -> 248;
        case 142 -> 29;
        case 143 -> 214;
        case 144 -> 37;
        case 145 -> 92;
        case 146 -> 145;
        case 147 -> 115;
        case 148 -> 0;
        case 149 -> 36;
        case 150 -> 85;
        case 151 -> 44;
        case 152 -> 133;
        case 153 -> 160;
        case 154 -> 52;
        case 155 -> 154;
        case 156 -> 226;
        case 157 -> 234;
        case 158 -> 54;
        case 159 -> 87;
        case 160 -> 186;
        case 161 -> 100;
        case 162 -> 64;
        case 163 -> 216;
        case 164 -> 17;
        case 165 -> 22;
        case 166 -> 102;
        case 167 -> 47;
        case 168 -> 247;
        case 169 -> 217;
        case 170 -> 219;
        case 171 -> 24;
        case 172 -> 147;
        case 173 -> 1;
        case 174 -> 192;
        case 175 -> 21;
        case 176 -> 128;
        case 177 -> 46;
        case 178 -> 66;
        case 179 -> 163;
        case 180 -> 146;
        case 181 -> 93;
        case 182 -> 180;
        case 183 -> 62;
        case 184 -> 230;
        case 185 -> 114;
        case 186 -> 79;
        case 187 -> 211;
        case 188 -> 183;
        case 189 -> 126;
        case 190 -> 61;
        case 191 -> 67;
        case 192 -> 26;
        case 193 -> 156;
        case 194 -> 198;
        case 195 -> 40;
        case 196 -> 32;
        case 197 -> 212;
        case 198 -> 195;
        case 199 -> 99;
        case 200 -> 88;
        case 201 -> 50;
        case 202 -> 28;
        case 203 -> 175;
        case 204 -> 101;
        case 205 -> 107;
        case 206 -> 250;
        case 207 -> 201;
        case 208 -> 141;
        case 209 -> 239;
        case 210 -> 169;
        case 211 -> 96;
        case 212 -> 117;
        case 213 -> 196;
        case 214 -> 178;
        case 215 -> 16;
        case 216 -> 221;
        case 217 -> 12;
        case 218 -> 181;
        case 219 -> 13;
        case 220 -> 174;
        case 221 -> 233;
        case 222 -> 224;
        case 223 -> 155;
        case 224 -> 218;
        case 225 -> 76;
        case 226 -> 215;
        case 227 -> 246;
        case 228 -> 90;
        case 229 -> 142;
        case 230 -> 106;
        case 231 -> 121;
        case 232 -> 238;
        case 233 -> 69;
        case 234 -> 82;
        case 235 -> 33;
        case 236 -> 241;
        case 237 -> 162;
        case 238 -> 84;
        case 239 -> 119;
        case 240 -> 161;
        case 241 -> 55;
        case 242 -> 244;
        case 243 -> 188;
        case 244 -> 41;
        case 245 -> 213;
        case 246 -> 151;
        case 247 -> 72;
        case 248 -> 70;
        case 249 -> 123;
        case 250 -> 42;
        case 251 -> 206;
        case 252 -> 202;
        case 253 -> 199;
        case 254 -> 97;
        default -> 200;
      };
      // Dynamic end

      // Calculate the first part of the decryption key
      int keyPart1 = (key2 & 0xFF) - initialKey;
      if (keyPart1 < 0) {
        keyPart1 += 256;
      }

      // Calculate the second part of the decryption key
      int keyPart2 = ((key2 & 65535) >>> 8) - initialKey;
      if (keyPart2 < 0) {
        keyPart2 += 256;
      }

      // Decrypt the obfuscated characters using the calculated keys
      for (int i = 0; i < obfuscatedChars.length; i++) {
        int keySelector = i % 2;
        char currentChar = obfuscatedChars[i];

        // Use keyPart1 or keyPart2 based on the keySelector
        if (keySelector == 0) {
          obfuscatedChars[i] = (char)(currentChar ^ keyPart1);
          keyPart1 = ((keyPart1 >>> 3 | keyPart1 << 5) ^ obfuscatedChars[i]) & 0xFF;
        } else {
          obfuscatedChars[i] = (char)(currentChar ^ keyPart2);
          keyPart2 = ((keyPart2 >>> 3 | keyPart2 << 5) ^ obfuscatedChars[i]) & 0xFF;
        }
      }

      // Cache the decrypted string
      stringsCache[index] = new String(obfuscatedChars).intern();
    }

    return stringsCache[index];
  }
}
