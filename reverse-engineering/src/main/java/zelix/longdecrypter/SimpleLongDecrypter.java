package zelix.longdecrypter;

import java.util.ArrayList;
import java.util.Vector;

public class SimpleLongDecrypter implements ILongDecrypter {
  private long inputKey;
  private int[] encryptionInts;
  private ILongDecrypter child;
  private long key;
  private long[] longNumberPool;
  // It holds all 64 powers of two: 1, 2, 4, 8, 16, 32, 64, 128, 256...
  private static long[] POWERS_OF_TWO = new long[64];
  static int[] mutableEncryptionInts;
  private static ArrayList<ILongDecrypter> cachedLongDecrypters;
  private static Vector<Class<?>> lookupClasses;
  private static int cachedDecryptorsSize;
  private static Object someObject;
  private static int CONST_52 = 52;
  private static int CONST_128 = 128;
  private static int CONST_17 = 17;

  public static Object getSomeObject() {
    return someObject;
  }

  // a
  public static ILongDecrypter buildNumberDecryptor(long mainKey, long fallbackKey, Class<?> lookupClass) {
    FallbackLongDecrypter.a(mainKey > 0L); // ??
    ILongDecrypter mainDecrypter = createNumberDecryptor(mainKey);
    ILongDecrypter fallbackDecrypter = createNumberDecryptor(fallbackKey);
    ILongDecrypter var7 = FallbackLongDecrypter.getPairStatic(mainDecrypter, fallbackDecrypter);
    if (lookupClass != null) {
      // Unused
      lookupClasses.add(lookupClass);
    }

    return var7;
  }

  static ILongDecrypter getCachedDecrypter(long key) {
    int index = (int) decryptNumber(key, CONST_52, 63, mutableEncryptionInts, POWERS_OF_TWO);
    if (index < cachedDecryptorsSize) {
      return cachedLongDecrypters.get(index);
    } else {
      if (cachedLongDecrypters.size() % CONST_128 == 0) {
        mutableEncryptionInts = mutableEncryptionInts.clone();
      }

      SimpleLongDecrypter var3 = new SimpleLongDecrypter(key);
      cachedLongDecrypters.add(var3);
      return var3;
    }
  }

  private static ILongDecrypter createNumberDecryptor(long key) {
    return new SimpleLongDecrypter(key);
  }

  static void initSharedDecrypter(FallbackLongDecrypter sharedDecrypter) {
    cachedDecryptorsSize = cachedLongDecrypters.size();
    sort();
    sharedDecrypter.init();
  }

  static void initEncryptionData(FallbackLongDecrypter sharedDecrypter) {
    sort();
    int[] var10000 = mutableEncryptionInts = new int[64];
    var10000[0] = -21;
    var10000[1] = -18;
    var10000[2] = -31;
    var10000[3] = -38;
    var10000[4] = -14;
    var10000[5] = -15;
    var10000[6] = -7;
    var10000[7] = -7;
    var10000[8] = -26;
    var10000[9] = -13;
    var10000[10] = -45;
    var10000[11] = -36;
    var10000[12] = -36;
    var10000[13] = 7;
    var10000[14] = 7;
    var10000[15] = -8;
    var10000[16] = -43;
    var10000[17] = -39;
    var10000[18] = 14;
    var10000[19] = 18;
    var10000[20] = 15;
    var10000[21] = 21;
    var10000[22] = 13;
    var10000[23] = 8;
    var10000[24] = -15;
    var10000[25] = -12;
    var10000[26] = -14;
    var10000[27] = -5;
    var10000[28] = -10;
    var10000[29] = -33;
    var10000[30] = -15;
    var10000[31] = -23;
    var10000[32] = 5;
    var10000[33] = 31;
    var10000[34] = 26;
    var10000[35] = -25;
    var10000[36] = -27;
    var10000[37] = 12;
    var10000[38] = 10;
    var10000[39] = 15;
    var10000[40] = 14;
    var10000[41] = 38;
    var10000[42] = -9;
    var10000[43] = -18;
    var10000[44] = -6;
    var10000[45] = 15;
    var10000[46] = -12;
    var10000[47] = 36;
    var10000[48] = 36;
    var10000[49] = -3;
    var10000[50] = 6;
    var10000[51] = 9;
    var10000[52] = 3;
    var10000[53] = -4;
    var10000[54] = 23;
    var10000[55] = 45;
    var10000[56] = 39;
    var10000[57] = 4;
    var10000[58] = 12;
    var10000[59] = 43;
    var10000[60] = 25;
    var10000[61] = 18;
    var10000[62] = 33;
    var10000[63] = 27;
    sharedDecrypter.prepareCachedDecrypters();
  }

  private SimpleLongDecrypter(long inputKey) {
    this.inputKey = inputKey;
    this.encryptionInts = mutableEncryptionInts;
    this.longNumberPool = POWERS_OF_TWO;
  }

  // a
  @Override
  public long decrypt(long decryptKey) {
    long var3 = this.decryptNumberUsingThisKey(8, 55);
    // Generate new input key
    long var5 = this.inputKey ^ decryptKey ^ this.key;
    this.inputKey = var5;

    if (this.child != null) {
      // Update input key of children accordingly
      System.out.println("child: "+ child);
      this.child.decrypt(decryptKey);
    }

    return var3;
  }

  @Override
  public void setKey(long key) {
    this.key = key;
  }

  /**
   * Set child that will be decrypted AFTER this decrypter being decrypted
   */
  @Override
  public void setChild(ILongDecrypter child) {
    if (this != child) {
      if (this.child == null) {
        this.child = child;
      } else {
        this.child.setChild(child);
      }
    }
  }

  @Override
  public int hashCode() {
    return (int)this.hashCodeFromDecrypted(8);
  }

  @Override
  public boolean equals(Object var1) {
    if (this == var1) {
      return true;
    } else {
      return var1 instanceof SimpleLongDecrypter ? this.hashCodeFromDecrypted(56) == ((SimpleLongDecrypter)var1).hashCodeFromDecrypted(56) : false;
    }
  }

  @Override
  public boolean lessThanOrEqual(ILongDecrypter other) {
    if (this == other) {
      return true;
    } else {
      if (other instanceof SimpleLongDecrypter) {
        long thisLong = this.decryptNumberUsingThisKey(56, 63);
        long otherLong = ((SimpleLongDecrypter) other).decryptNumberUsingThisKey(56, 63);
        long diff = thisLong - otherLong;
        return diff <= 0L;
      } else {
        return true;
      }
    }
  }

  @Override
  public int[] getEncryptionInts() {
    return this.encryptionInts;
  }

  private long hashCodeFromDecrypted(int var1) {
    return this.decryptNumberUsingThisKey(0, var1 - 1);
  }

  private long decryptNumberUsingThisKey(int skipRightShift, int skipLeftShift) {
    return decryptNumber(this.inputKey, skipRightShift, skipLeftShift, this.encryptionInts, this.longNumberPool);
  }

  private static long decryptNumber(long key, int rightShiftSkip, int leftShiftSkip, int[] encryptionInts, long[] unusedArray) {
    long var6 = 0L;
    int length = encryptionInts.length; // Always 64 length

    for (int i = 0; i < length; i++) {
      // We can also represent "POWERS_OF_TWO" in binary form like this:
      // 1, 10, 100, 1000, 10000, 100000... and it holds 64 of them

      long powerOfTwo = key & POWERS_OF_TWO[i];
      long encryptionInt = encryptionInts[i];
      if (powerOfTwo != 0L) {
        if (encryptionInt > 0) {
          powerOfTwo >>>= encryptionInt;
        } else if (encryptionInt < 0) {
          powerOfTwo <<= ~encryptionInt + 1;
        }

        var6 |= powerOfTwo;
      }
    }

    byte arrayLength = 64; // = encryptionInts.length
    long result = var6;
    int leftShiftAmount = arrayLength - 1 - leftShiftSkip;
    if (leftShiftAmount > 0) {
      result = var6 << leftShiftAmount;
    }

    long rightShiftAmount = rightShiftSkip + arrayLength - 1 - leftShiftSkip;
    if (rightShiftAmount > 0) {
      result >>>= rightShiftAmount;
    }

    return result;
  }

  /**
   * Sorts long decrypters ascending using merge sort algorithm. It sorts by the return value of {@code decrypter.decryptNumber(56, 63)}
   */
  private static void sort() {
    byte var0 = 0;
    sort(0, cachedLongDecrypters.size() - 1, cachedLongDecrypters, new ArrayList<>(cachedLongDecrypters), var0);
  }

  private static void sort(int startIdx, int endIdx, ArrayList<ILongDecrypter> mutableLongDecrypters, ArrayList<ILongDecrypter> longDecryptersClone, int iteration) {
    if (startIdx < endIdx) {
      int middlePoint = startIdx + (endIdx - startIdx) / 2;
      if (++iteration < CONST_17) {
        sort(startIdx, middlePoint, mutableLongDecrypters, longDecryptersClone, iteration);
        sort(middlePoint + 1, endIdx, mutableLongDecrypters, longDecryptersClone, iteration);
      }

      merge(startIdx, middlePoint, endIdx, mutableLongDecrypters, longDecryptersClone);
    }
  }

  private static void merge(int startIdx, int middlePoint, int endIdx, ArrayList<ILongDecrypter> mutableLongDecrypters, ArrayList<ILongDecrypter> longDecryptersClone) {
    int currentIdxFromStart = startIdx;
    int currentIdxFromHalf = middlePoint + 1;

    for (int i = startIdx; i <= endIdx; i++) {
      longDecryptersClone.set(i, mutableLongDecrypters.get(i));
    }

    while (currentIdxFromStart <= middlePoint && currentIdxFromHalf <= endIdx) {
      ILongDecrypter var8;
      ILongDecrypter decrypterFromStart = longDecryptersClone.get(currentIdxFromStart);
      ILongDecrypter decrypterFromMiddle = longDecryptersClone.get(currentIdxFromHalf);
      if (decrypterFromStart.lessThanOrEqual(decrypterFromMiddle)) {
        var8 = longDecryptersClone.get(currentIdxFromStart++); // Smaller
      } else {
        var8 = longDecryptersClone.get(currentIdxFromHalf++); // Smaller
      }

      mutableLongDecrypters.set(startIdx, var8);
      startIdx++;
    }

    while (currentIdxFromStart <= middlePoint) {
      mutableLongDecrypters.set(startIdx, longDecryptersClone.get(currentIdxFromStart));
      startIdx++;
      currentIdxFromStart++;
    }
  }

  static {
    long currentBit = 1L;

    // Generate
    for (int i = 0; i < 64; i++) {
      POWERS_OF_TWO[i] = currentBit;
      currentBit <<= 1;
    }

    someObject = new Object();
    lookupClasses = new Vector();
    cachedLongDecrypters = new ArrayList();
    initLongDecryptors();
    cachedDecryptorsSize = cachedLongDecrypters.size();
    sort();
  }

  private static void initLongDecryptors() {
    mutableEncryptionInts = new int[]{
        -47,
        -29,
        -20,
        -12,
        -58,
        -11,
        -32,
        -48,
        -16,
        -31,
        -27,
        -34,
        -32,
        -26,
        -27,
        12,
        11,
        -26,
        -32,
        -4,
        -37,
        -42,
        20,
        4,
        16,
        -28,
        -7,
        -4,
        -24,
        -32,
        29,
        4,
        -10,
        7,
        -26,
        -13,
        -10,
        27,
        32,
        26,
        31,
        27,
        10,
        26,
        32,
        34,
        10,
        47,
        13,
        -5,
        32,
        -8,
        24,
        28,
        5,
        48,
        -2,
        37,
        2,
        8,
        26,
        32,
        58,
        42
    };
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6744423634385984943L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6706934200393210049L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6990400485665549342L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8478105918229553425L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4523791493794833373L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6956550957728802162L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6378052640071885957L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8115039236204621966L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3404969271164910260L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6837460718211466928L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(909643428573496924L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2853620746374076905L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7558632103745708878L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4451995920286813933L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2987343977142760299L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8774831055784385353L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2491703159784603645L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3825761391279259103L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8103271119208899000L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3751748464649851844L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4655894350249038690L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2299116995506572841L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4248253892470355672L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(673339327068868620L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(928988291849119595L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4494707613054168802L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8219881982464703334L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1592580442844519725L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6521590094206246995L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7862763716460523474L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7180250963500505561L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1990481056537250245L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4259418252620178920L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(115230540288189211L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-260614310560079233L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4560237432271780805L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5488233296441099736L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6602289711462896865L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6238345315009356580L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4887668945434071164L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7080207893442515422L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4442284130421016109L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8540532653575655206L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4153213651530454446L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4650655848889566882L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8172887638636256619L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(285257623463465760L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2212661351872423105L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8955014614648852638L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7685371999257522432L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7480559610018095299L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7645975422665949738L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8502689991448483739L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4027701581277325167L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1632673887745594740L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6478213330387212258L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5284458860625299893L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4188699928120191279L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1672758662401941534L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5269287403847926315L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6948122180030299767L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7605450297947617089L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8309140045178447544L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5320132290067212846L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4793472161571137674L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(335755845515907412L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8229858481322148701L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-481794707622686634L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6395100604994942360L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6119572320851432877L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2184643316460692798L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8981503674350135997L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7656732516946638003L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5635213181667554701L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1151223719834730898L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-611382292272610294L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7885244854120664817L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1657074236485762147L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5902840883215474855L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6615869915130831983L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3088267513636873593L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3461658328266714253L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6629250593412551926L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1996215071285581834L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5002744937498769605L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6355238420635454613L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5181722811629877386L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6883115128543829165L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6088188338395302712L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2611888656600445821L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4646520290036149891L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4378219892565776629L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8976129125876155436L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8363523088435887098L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(86840831297761602L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7787591308013543461L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3469387858000844866L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5233032057401438375L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-9118749419407424250L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8500163652186440446L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3757520509916313379L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4116244975720934845L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6178501944032822413L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-9153356831469675727L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1063398336654163675L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(770696137606370564L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8186032946492650175L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1593212751750682921L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5956464615920267193L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8015458933371737043L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5554639710744978350L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3650944509352886554L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3169510854341612216L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1828008516145470950L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6789118416236927168L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4762700133102726812L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8878911884416891471L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8664621867198439947L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8916388788512143153L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4412593360637248002L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6927240979754877261L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1216636158766706064L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7497317701947733992L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5868253207908670304L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8271568981229458725L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1582435105153641742L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1003193982455307812L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3744940415362494952L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8057241827569097198L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8160938211100963327L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5426810931933071203L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3484536533526790993L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8909305946017264730L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-607288752669276128L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1500707136245063922L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1112894094489036189L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2186260659538304726L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8497316323465943560L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2378738370292703073L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4256453459033539335L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7399179070313343094L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4994067841559076140L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(565607164416017254L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4047108070709861516L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1857416293356308444L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4048018357712373000L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5197029575525773668L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3547717426064585430L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4760024407670384218L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(313069990748533254L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5348539643686660003L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7272107145001041857L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4615823146970798776L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6802957569116356865L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(109198422163880778L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(719550753215290476L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(663070648437475965L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7982366021878349477L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3154350453178068187L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7079747071937835226L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4852688329732284852L));
    mutableEncryptionInts = (int[]) mutableEncryptionInts.clone();
    cachedLongDecrypters.add(new SimpleLongDecrypter(6924729867076140072L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1319055387195296970L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1110718098836545406L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5184487272089418201L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1209929019013289676L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3934943933646132417L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6612345466967366939L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7983784829243525038L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3465473421090667983L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4955069875975476330L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1222738085821493094L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7274631535676754294L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3605167428510276924L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7321401848686268006L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8537181269155846398L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7036010071487345011L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6469190484441503783L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(358554658493788272L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4801186158922092356L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2025941902864584272L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5046251421333692002L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4346213119371418670L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1952490212279718209L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(388904733011491916L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8032609794616146291L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5947827916444804545L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1529830667070193801L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6425974338071218780L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3889257601660206246L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3534689640069068320L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6044722655579239339L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4849810454304746038L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5622382021455524496L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2019625191226615731L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4709691165499111106L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1792484731624864873L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7281842097693247804L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-60290756861186714L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6474293750210062665L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1393182141334933255L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4414579491870249421L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4841279960438382325L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2662755756897883207L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-333062117895070857L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7651854756009155114L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6649917936615431635L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2215331531862059131L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4626100401364144631L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8597788376328339537L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5419944576110703776L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1647514364380703233L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4913700706030770261L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2972229743455703418L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7097384657830585849L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5866054288827446814L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2635971902479150421L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1234291004083169172L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6296365828022667262L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6198154523778839226L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7280882661630946318L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1163246228181812916L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8058917293246814304L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2555097195074984218L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5238458381956505444L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5274531756641706705L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4778378953595314612L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5443240005591331681L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1922697014945020873L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-758164801927635189L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3750102876210478271L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(922384580518357556L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5346287844412991411L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6937062962655216499L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5479188277678227209L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6355320449421308357L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(773272052063831344L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2250951039312121397L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7509361718814825052L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1064803118799081080L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8090526503937156583L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4660199197585511367L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4990315584293803368L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1929101553301683300L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3706831602997022299L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3244945072854826617L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6721406422293792749L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-376202964417428685L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3020618294975159574L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2524768609197567760L));
    mutableEncryptionInts = (int[]) mutableEncryptionInts.clone();
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5106164513165795732L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-673701620128658570L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3032839098405056378L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8157567941448647257L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2322221287690188288L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-933042621612592580L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8699774343065680999L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2037709077370185054L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-25683838325834428L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2092240939937505885L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1775291314990744973L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1636075790375377310L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1936241699616714117L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5777575772619753739L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4442815237220284737L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1360860040086278686L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3844824520896299857L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8075846110590462860L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6750562378065394424L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2071856796292556699L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7645975440382689850L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1973643837185670230L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6596323918647719115L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5505711763130324890L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-590881294597069820L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5149094331846857342L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6649986424440623541L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-475936916104918188L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5164782238865553122L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8366919996635922256L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3160648661163137486L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-880428123426138517L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4425756912056562425L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4887734184666877140L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6240037117283760480L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1690480913210458466L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3491202511124849912L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8799598494005851568L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7322312843080944694L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6842343091803594885L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4600781611143661320L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2501933747265243007L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(157513467097159432L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-51397667266540032L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5197404301377746114L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5561856049065377315L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6847457923712613988L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(690810815984985962L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6910462901404226130L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(9066171932773458292L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2979620315348532580L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(966601938649563477L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1979829244037265369L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6138508966444145039L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7285017837801602088L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5337906342982701542L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5698217665490279274L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6279771978459474340L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(306438502956140149L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6022601678053684299L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6247038219811084434L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3485642966629178362L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-280204998506807255L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(811953170445661739L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6676356884277843262L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4203274232037917986L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6218698579016947227L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(9062922882051374362L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7392999613859714256L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1951270258536690178L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6475368503852093309L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6094943361180325249L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8238626769688455661L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8491508749498464741L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1755966378406569721L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8524881805879043193L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-628980067508333726L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7553595808090830267L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3401088427898417385L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8148495414709501776L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2391241009525101111L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-846777915758294233L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-757066702853567984L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7858483658340044671L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8765913414195489930L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1071410693439370419L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6937867782310580346L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4951001095487041465L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7185266059361506062L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2748102153912659494L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4738100879865434414L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3145300095240542486L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6535859322297191350L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4507200759147770011L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1740123166599572719L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-9219816827940516490L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(591935923207441183L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6931212079319678924L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1927951755889431824L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1018125983805817356L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1713219551072842643L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1993225270065169106L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1497482565846420089L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8419134277282359775L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1016119785387431222L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7860660830204724684L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8223057368263223413L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3672232568321311139L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8928735346180880288L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5703536978811697351L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4373616876358252673L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5496240696206782572L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8911596651907001674L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8466133811519483080L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7036095350088556279L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6439300964726081222L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7559953241327815772L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8509983995108710244L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8126543840463541282L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4679530916572832059L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3315415731403675506L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6563689203004965384L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6399952368373559232L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6970545074926306033L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(9206226304900513604L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4183589437500349814L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(704930295637337170L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6291063742149989843L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6861793193461154999L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5213481917054572740L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6202798052391301836L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5227330657153389860L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7861980581239952747L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6411638694933490114L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7218582584421862135L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2456101936301468395L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7309193604905058320L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1169766170650380281L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5149765774756432886L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3619941783783821365L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4755953108808997225L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5898742921671940225L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1513806074914163381L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(213638593212357751L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1941308327608399155L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8652072385151148020L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5518343174315310225L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6668391554757700701L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3378303108682973236L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(153551912349589258L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7174519294373221017L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5576411236907470600L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3922072894231107391L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3084936538615937343L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-365481539812195825L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-9074392047666671583L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5252333847380776371L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8392327288880097969L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3749904315140114141L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(917548018462944056L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4410000888652955656L));
    mutableEncryptionInts = (int[]) mutableEncryptionInts.clone();
    cachedLongDecrypters.add(new SimpleLongDecrypter(4309931546848104734L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7825918609744072054L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2034815114218434608L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6815058800263487518L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6043575486452002712L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4170086999604523937L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2831257470140081013L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7073463272410348565L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6837280645850915462L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3780232701099159317L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7827162877968932208L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8876862564686567611L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1816290325167492872L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6084032547809917278L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2004026282083097840L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5681691551577807424L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1362384365585262897L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8254130692795050797L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(95854038055961911L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7584561608549564522L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2225803302692197L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7925858458233475061L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-783063673896965100L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6474177126950886066L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1424906516450865987L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8994706141977867292L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5427762163764242642L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(752633996393504890L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3821773867277842895L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5044365075408941478L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4334696141216550490L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5223622137639054093L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3747734116506103956L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3476627681176321150L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6701728281633932876L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7879316057935635805L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1961540790445216207L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5609925979471008083L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2758595237753181999L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3828868950989022787L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7554970856115997762L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1674289599311139799L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5882541554291952257L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5970517969145226666L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5694327872563891786L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-9075081430626130678L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(510363649096219245L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2932043914224158007L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2103310732740451281L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(9143091788327051278L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8190331378858654425L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6484515748255825118L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-989102733826402503L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2805589460348619455L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4952624266317925170L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7866602387523759598L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2838535743579155169L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(9130337089540320078L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6155941245566701791L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4369077573250708827L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3403922283335250125L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5228145801860091857L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5611646657723339879L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3727659461417825161L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8098452576981511931L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5840023793533036775L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(305061427025647218L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1554589124966828544L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-995902213687802079L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1661298148894725234L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1171190151657088490L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1911122725552527299L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4166212015522712752L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1307637690161925921L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4807711348563110904L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7911655230983048329L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6183208992543283055L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1274010569969155473L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-248690450166941417L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(438974322021211749L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8521068021660184464L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3156900595960784616L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(946611373396269568L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3330553286879175391L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3003257089522996293L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5539589064221234295L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6147579707562708687L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6271026328840650696L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8038276827517709911L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6398103833070748828L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3657318447125409469L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8383621622363403705L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8216605443336037219L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8713102081639797436L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7543937831586873292L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8993887115287846087L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(857243566681711432L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6877473343732551430L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6442566549481596084L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8346392322474908715L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8950600426892419611L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2251462301258663597L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8235262215580202595L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2181622447026851046L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3920438834459637895L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6962082836595194640L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4148519272451968693L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-161146386391559860L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2665459711608506848L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3709258874196377632L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6039434875928637786L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2178029681318653626L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-744779604243454667L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-9024143963715833852L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2691682731458806928L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7018807265652199746L));
    mutableEncryptionInts = (int[]) mutableEncryptionInts.clone();
    cachedLongDecrypters.add(new SimpleLongDecrypter(-919413858743417798L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1774623754234802882L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8339418576012490680L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7513479833408246487L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2314818037513073106L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-9074274855793584370L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3277659344386098540L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8045473141863789512L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7970299096669443799L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1034754903083837051L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3100201980261012324L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3701670125437049291L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5653162943558428452L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1625458280817133288L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(9168198672079513207L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1756840750465306970L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3563527016006297273L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3124357265138734874L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-766134785560780261L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3590677551555116172L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1580854947335065240L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3679035563956529320L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4131972852617518234L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1916171663225653717L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8888138375515087806L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(837547871646678133L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8390388916778336276L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5285180317943538453L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-9195622821895535786L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4568494496855487641L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1723762583039395626L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3656784539222589676L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5045168601700388826L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2029989707988253900L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5590402094919585524L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4465196013497954929L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7131193391680124420L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1457143371037150647L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2025195411394300269L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2392720093380125878L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-9180298646049917071L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6829077588024813969L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3590114192596026103L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4177899441345042019L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1932025508841098613L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4374414213058128253L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2635133127341295326L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1212775154779584214L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2077230865184567803L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8532627771998088723L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1918301575936345551L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6181119479344910834L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7607659780887766874L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8841419320954119994L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3386952276780610202L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7265218758822181265L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6885763395958973390L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7175179636323001898L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(965306621268280864L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8964937495528998351L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7945823546398330117L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6582853383060287522L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3013505789382482932L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6345532957235816740L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7717839717785972351L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(303352763970566936L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3993071653855238260L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6355371785498034955L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4409817751224932099L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5025307611986570656L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-516646206414250491L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-207387019995361789L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5930061197256787957L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4664346185322264172L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8975714574788740655L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(9074650266398893630L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4670921149196815655L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2065487589269618250L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8002737148078177618L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7613899854096587131L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8363971138228398118L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-336398731228566917L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6067865635786431478L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-537350056175380937L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-821403342938294173L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1388933875687454829L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1811447339585681064L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5723306975138455595L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-917198550118498522L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2067564707638383745L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4617416675770205927L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3256576131634881107L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8510773107491051585L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7365138421256309114L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(163571525502003729L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8160441735082537413L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-9127840326063847917L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8489083611002688562L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6744934502626892772L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2221286548225247325L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2962263413232090336L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3934146036784200195L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3436771769251688178L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-571685545082449314L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3945694407626549965L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8845643057329656427L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-114004742351245484L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7563862587164857524L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5389753993406410661L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-160132178394707695L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6183523531436058364L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-214828784118509446L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1772884462377548962L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8949315185481553926L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3729103974117941717L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4068251328299929969L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-997635462382512347L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1317888929730875933L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7127247260880184787L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5537235031471907997L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5877154569661868784L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(915032465674082373L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3696594469398488735L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1198701298961969789L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8641144559498016152L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4064835476831462925L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2841196238079137667L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3099776064505963192L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2939614047086402172L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(9071330913751975238L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5120371539735949397L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2962658612416286234L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6689004982062057288L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8964882758012483870L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5475469752228117537L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2984707355842012321L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5597108390879478379L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(394761154282362208L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1230930935480479505L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8396531332138972334L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8950390791323304538L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7341335208455734582L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2511358839460762368L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8749169917523618680L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3629581727734455696L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2494960099271702435L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1333200973290266984L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5220533190940070166L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7182310048075607823L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4992762815153309925L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1567701269007552940L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6233143081381465527L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5356460109429705702L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2921817814179584568L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5157239356123225381L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6545866594900197983L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1723419563101916175L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1768072117978956882L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1697426504002076120L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4693519658482978727L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6477458318850806217L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2578676782114461676L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4822563471147350265L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4644685633478581922L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2077374908101105818L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7772646909634991452L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4802873170356544924L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4732105155733304966L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3191653724556036960L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6675909975872865506L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4370639611625329080L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6127069360154651222L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3830571812126989260L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1145135241143221176L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5757803355491053851L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5861514532574319479L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5029961575385880373L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4737356065818941110L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1997637500916576252L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-402317396686749838L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5490296329405198209L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7015982033247445319L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5566428372603202648L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1610547181022171706L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-749958015179314652L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3287041484289893793L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1205591141138602038L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3244013406560206393L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4381897587511407242L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2769134562871130661L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5510210567648891639L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6315616873848322948L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3704471921972883308L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-628270213006929303L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2929554665206796590L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5394171273559455145L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5396731502047557482L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4573704703133599927L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(380442719608947285L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4819409575663658013L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6093821547247321433L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1244195524525771376L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1490733409722229745L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7846179103121209336L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6793493318428248099L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1878180836146942289L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(9127210875299794782L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3121621012635395049L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5627575452836567811L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4345720246612391945L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2987207132099937157L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2718525029608824611L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1928462778601061980L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8726174620560326418L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3742312471085449325L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(9172196962600855678L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8457539779541178549L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5148847283195559037L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6287796511558536662L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6728069329333144688L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-728759495188423120L));
    mutableEncryptionInts = (int[]) mutableEncryptionInts.clone();
    cachedLongDecrypters.add(new SimpleLongDecrypter(3123981817461308697L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-9069789736011624704L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-323664384510299095L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7371305299622091599L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5118654025279893444L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8914889255355890013L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6341878152146381740L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8719301639867663453L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1447485275231485462L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3000218305613173085L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5628421437479278969L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-464022640854493676L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4503733056757245403L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5119989142457130753L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(9057318678582108161L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1490790573133747500L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6526284619751243769L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7948142709974031419L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-166781528820860339L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2097328693574984093L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4846623685971182474L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7404752823167592293L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4655249128411810447L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3324069469948004682L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3682584884556362398L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2634301777958225283L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2019191113204726620L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-9095504018929749741L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7768597088751846295L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7827637957870423658L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2590720253343633763L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2074445615499076596L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4478877478060189823L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3871648429399759137L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5780224641161271655L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-587307018214525586L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4713977051001056438L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8591023449897235973L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6496683859634571100L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-472878060695501236L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3840600464305262995L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3589405155502518259L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2763248056044434997L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4692043840546359380L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1848123464957624244L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2045980452423856442L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-117892202448721551L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5697288768445779184L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2276005181197399426L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(9174021919578190095L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8414178418449687143L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(964909248668401510L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6420089835907853503L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1206668257124389910L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5228800532249141616L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4369205978830936126L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7729861559149491830L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4683709738109657062L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8134414797632320240L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1710424857346228286L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4533787663450792213L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5250712491123428927L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7399998953575112158L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7503459437579941063L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1759656563388384806L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6049160167533009731L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5662735568805587576L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3287329673264815835L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7202674938420357883L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8264077325419788549L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(105829686699111191L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8639859108793276933L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7929899358064994728L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5603830156674993046L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3065385947166298415L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1590803408820128797L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8068534516492922392L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8006462752586302146L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4415286755741161732L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7579467872633793618L));
    mutableEncryptionInts = (int[]) mutableEncryptionInts.clone();
    cachedLongDecrypters.add(new SimpleLongDecrypter(3353689417665180214L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4890728051061267630L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7765364972116597192L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2950975812271981995L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1865222533820919226L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5935273144739166183L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8726441470434619326L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6537978008560483175L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4729355686354735841L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5261917703163211105L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(382694519422632533L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6230649450814295490L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7011501018073908618L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8480807668178964840L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7316813999135947873L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1978630551954399630L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3450396147663272072L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8566344773080147277L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7130493993344824022L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3074040277587997737L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3215216590703372718L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(668392685647621974L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8299355380725830290L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2141160178662731760L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5552714407979987888L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8514174249124482292L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-267538969170167187L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6015945704533227926L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2848645146241511730L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8732403067646632451L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7447266920259331606L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3746081854878348656L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(9027048670226612759L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5150537824674051723L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8856255601204710879L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1603063408204147182L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4379963049119602342L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2354106643511466664L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8438222519059515827L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8525241284021267915L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4713631510949118869L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5494197952805683322L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2803714062836393466L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(834350807942076220L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2803025750978137237L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3639592030903621369L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3205221769422207620L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6137169999648429155L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1845155904071928601L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1881462987446087967L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8994150716039412784L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-689266450110917261L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2870767467022586685L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3416893677099904837L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5604288906048632072L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4794574540075488122L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8873369584428817349L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4199752890653780871L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1746150380386033081L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7275157191837229502L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1464668239948125706L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1989670409584258754L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4100623189401528040L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-9022342751689801679L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1297874714300681867L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6984393055181507250L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-859300423905213946L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5175754230219423683L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4679689865775202595L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2848532333699623434L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6101318258513184912L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4916973882150029207L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6277451035331749555L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(980234207379701280L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6620208133880536149L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3363289285053783387L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3175150297517903003L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8600416641602506940L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3299980457845877485L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2123386905965069918L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6114029935015278708L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3371568838790535065L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5278880873848158683L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3620838319557240908L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8316126234860352408L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2128711266520349557L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1660862800821763670L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8027156411800809878L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1090949195215716356L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3849647630193561094L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5254885491080214472L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6204725911977086024L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6707797091292015721L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1988124221269519528L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5092529663763261197L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2016028107744740408L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2900481677748932119L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(648173356358948150L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4226564601689169858L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4335322383944857512L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6016061986573194912L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7176060433647764159L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2942344384811892567L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6092988999849727017L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3945225378372948229L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5645814257619978902L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1785667623295804164L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5101505602929939221L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5487859664203196249L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5720851097316669648L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3785204615371068055L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-561986025123427258L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(470761896148187473L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-857798012934833099L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6330894209310279593L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7139268433438188557L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(9106837350205382004L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5411332552963557386L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7933151961286681106L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2892079223345912974L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6982561687063692631L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2719761414824180064L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8728583861384937694L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2321847920782466177L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2777521335515563352L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5004973603669270194L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(9163881514320632424L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4450156830168333756L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(9022710403794182422L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7843972757221422894L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8447990974203036899L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5878388923139080493L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4133067880340619408L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4343648983921339206L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5024685564347393621L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4866371535087567714L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2628180531315318072L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8972810740805988305L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8010469905465125831L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7154064171073873332L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1461356700224937042L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(98801013476167000L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-630565617635933577L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2923660292679716530L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8000052327432781205L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2877340485581057856L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2700898613642777271L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5199370076986222699L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7581939487537450731L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2179653435652298003L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1345552540003111049L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1027617938134274568L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8315145766711593684L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2780905536902388534L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1667862483904418024L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3583275351917121211L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4737090727306786044L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1331216929299961183L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1241739169435989729L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1021050309461968010L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8949170418377121573L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1027276572384110223L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-550100803298838720L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-920235981772234156L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1998750113306284382L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-9143248736720514975L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7818190187401558403L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3162165788155607192L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5414940549013959692L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4806474729330816058L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5314988679666910211L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6982668010006073115L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2465590116038820228L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6526893981964949074L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6194151172155161281L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8630599462842298898L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-22056709181439950L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7841026110869319964L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3431964442040488336L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4336759233672801839L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4818018289031333811L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1858801153554425710L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5407591726371152001L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(458869154562108713L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6120672096660523923L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1573077873219249141L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3400235870762790794L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8603852188278464302L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-216471682221518735L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4958488947543730786L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2617601700128523360L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1935099800016483434L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8851653639451196121L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(703647420220481895L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5829641327573384108L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6701836975809850721L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1728184293609821078L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1802124911314707856L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2649808558380673853L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8770921637106683093L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-9022305052714319615L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3641495095642281187L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4729485338924484294L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5163113304774476137L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6121189468112493070L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2260753270295613747L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5447368382983191900L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2783852683762409981L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2982911575606719503L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(418506796835376732L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4573654158771594273L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6089619787186807407L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8024073409900724384L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(9133873434019287659L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1348424155780968308L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6513276877409274716L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7817902869606204298L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7067165855987920604L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2002520142872203095L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6087898490378273711L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8743249348057432853L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5155296734268770562L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2383373866280366833L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5044812922513473382L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3078837164674525831L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4062729010651707550L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7857124971844231711L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5457968541179511401L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5756814311169447862L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2328398497679838418L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5037984042970560812L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5821246296106742657L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5598522630953587699L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6915720826048005328L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1757531524893795772L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7711366347596515359L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2508344940770375843L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1958901065989171903L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6593865677046990909L));
    mutableEncryptionInts = (int[]) mutableEncryptionInts.clone();
    cachedLongDecrypters.add(new SimpleLongDecrypter(4294148454648727092L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1403135501874259611L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(669894455170059330L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5969115259296313885L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6006074756119032913L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7087094550249005235L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1700128685116273294L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2701714164910619092L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3750132386748992080L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8055117132173116784L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8022141185680785037L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6003798324848002385L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7864245387436781866L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2432284427432697735L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5723260322880248553L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8231066441305293495L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5716782213702986430L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6065315107958657453L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8089463602689788585L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8267949031799818814L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-501014590016411067L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7417813050321278416L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8467561015506104600L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-953182606337653900L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4409572277925425069L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2984150602136508061L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2425815659940105290L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6202629222000764253L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3167090477094296677L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2375016374088837392L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1755159740002316903L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5816256118218694922L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8371596391687840489L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8946268129329818829L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7691106236346789404L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5347372426458345493L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-795585369112892359L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8589521086970508554L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5491431801700760458L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3653459661474739192L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(1965948575058990346L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4108480581253664294L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2525337852744631316L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3976816553450396890L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8903072313838479922L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2292313479567609205L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4722844739823544416L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7686699201968476699L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-6562554514220268235L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2320794911495513669L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7320299521338147397L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2805911437997031956L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(683020884408656649L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6126207642105232432L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7176016269284514297L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-8033835911069254896L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4521680277892617610L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7722464984245934535L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3864758102832358841L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3136819754763617106L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4612127509001437001L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(4877576988565574146L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-3993831536360412311L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3497127873665015327L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(810648566345487735L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(212666162098105862L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5248894219456725363L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3300269710224254300L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8846503112951411755L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(6109567338086971938L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5251722195395330154L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5330291147265649367L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(7565058400581352239L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4211887087490862059L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4939719026944369907L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-4983052400759416743L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3542674163069123398L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-1487119450460742355L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(3910139209092479572L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(5393760485569384472L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-7428051292777401773L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-5438727553116681666L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(-2649220214809800625L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(8597908996122952484L));
    cachedLongDecrypters.add(new SimpleLongDecrypter(2101404646864295476L));
    mutableEncryptionInts = new int[]{
        -13,
        -46,
        -2,
        -14,
        2,
        -33,
        -19,
        -17,
        -42,
        -30,
        -46,
        -18,
        -37,
        13,
        -23,
        -6,
        -24,
        14,
        -28,
        -11,
        -38,
        6,
        -32,
        -36,
        17,
        19,
        -10,
        -35,
        -6,
        18,
        11,
        -17,
        -29,
        -20,
        6,
        -28,
        10,
        23,
        33,
        30,
        24,
        -10,
        -2,
        -9,
        2,
        -15,
        28,
        46,
        17,
        37,
        42,
        10,
        9,
        20,
        32,
        -2,
        46,
        2,
        38,
        36,
        15,
        29,
        35,
        28
    };
  }
}
