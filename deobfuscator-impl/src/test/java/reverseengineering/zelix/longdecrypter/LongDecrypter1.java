package reverseengineering.zelix.longdecrypter;

import java.util.ArrayList;
import java.util.Vector;

public class LongDecrypter1 implements ILongDecrypter {
  private long input;
  private int[] numberPool;
  private ILongDecrypter parent;
  private long key;
  private long[] longNumberPool;
  private static long[] someLongNumberPool = new long[64];
  static int[] mutableNumberPool;
  private static ArrayList<LongDecrypter1> f;
  private static Vector<Class<?>> lookupClasses;
  private static int i;
  private static Object o;
  private static int CONST_1 = 52;
  private static int l = 128;
  private static int n = 17;

  public static Object g() {
    return o;
  }

  // a
  public static ILongDecrypter buildNumberDecryptor(long key1, long key2, Class<?> lookupClass) {
    LongDecrypter2.a(key1 > 0L); // ??
    ILongDecrypter var5 = createNumberDecryptor(key1);
    ILongDecrypter var6 = createNumberDecryptor(key2);
    ILongDecrypter var7 = LongDecrypter2.a(var5, var6);
    if (lookupClass != null) {
      // Seems like it should do checks if in sandbox, but this list is never queried. Who knows ¯\_(ツ)_/¯
      lookupClasses.add(lookupClass);
    }

    return var7;
  }

  static ILongDecrypter createNumberDecryptorForPair(long key) {
    int index = (int) decryptNumber(key, CONST_1, 63, mutableNumberPool, someLongNumberPool);
    if (index < i) {
      return f.get(index);
    } else {
      if (f.size() % l == 0) {
        mutableNumberPool = mutableNumberPool.clone();
      }

      LongDecrypter1 var3 = new LongDecrypter1(key);
      f.add(var3);
      return var3;
    }
  }

  private static ILongDecrypter createNumberDecryptor(long key) {
    return new LongDecrypter1(key);
  }

  static void a(LongDecrypter2 var0) {
    i = f.size();
    c();
    var0.d();
  }

  static void b(LongDecrypter2 var0) {
    c();
    int[] var10000 = mutableNumberPool = new int[64];
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
    var0.c();
  }

  private LongDecrypter1(long input) {
    this.input = input;
    this.numberPool = mutableNumberPool;
    this.longNumberPool = someLongNumberPool;
  }

  // a
  @Override
  public long decrypt(long decryptKey) {
    long var3 = this.decryptNumber(8, 55);
    long var5 = this.input ^ decryptKey ^ this.key;
    this.input = var5;
    if (this.parent != null) {
      this.parent.decrypt(decryptKey);
    }

    return var3;
  }

  @Override
  public void setKey(long key) {
    this.key = key;
  }

  @Override
  public void setParent(ILongDecrypter parent) {
    if (this != parent) {
      if (this.parent == null) {
        this.parent = parent;
      } else {
        this.parent.setParent(parent);
      }
    }
  }

  public int hashCode() {
    return (int)this.a(8);
  }

  public boolean equals(Object var1) {
    if (this == var1) {
      return true;
    } else {
      return var1 instanceof LongDecrypter1 ? this.a(56) == ((LongDecrypter1)var1).a(56) : false;
    }
  }

  @Override
  public boolean equals(ILongDecrypter other) {
    if (this == other) {
      return true;
    } else {
      return other instanceof LongDecrypter1 ? this.decryptNumber(56, 63) - ((LongDecrypter1) other).decryptNumber(56, 63) <= 0L : true;
    }
  }

  @Override
  public int[] getNumberPool() {
    return this.numberPool;
  }

  private long a(int var1) {
    return this.decryptNumber(0, var1 - 1);
  }

  private long decryptNumber(int var1, int var2) {
    return decryptNumber(this.input, var1, var2, this.numberPool, this.longNumberPool);
  }

  private static long decryptNumber(long key, int const1, int const2, int[] numberPool, long[] longNumberPool) {
    long var6 = 0L;
    int var8 = numberPool.length;

    for (int var9 = 0; var9 < var8; var9++) {
      long var10 = key & someLongNumberPool[var9];
      long var12 = (long)numberPool[var9];
      if (var10 != 0L) {
        if (var12 > 0) {
          var10 >>>= var12;
        } else if (var12 < 0) {
          var10 <<= ~var12 + 1;
        }

        var6 |= var10;
      }
    }

    byte var13 = 64;
    long var14 = var6;
    int var11 = var13 - 1 - const2;
    if (var11 > 0) {
      var14 = var6 << var11;
    }

    long var15 = (long)(const1 + var13 - 1 - const2);
    if (var15 > 0) {
      var14 >>>= var15;
    }

    return var14;
  }

  private static void c() {
    byte var0 = 0;
    a(0, f.size() - 1, f, new ArrayList(f), var0);
  }

  private static void a(int var0, int var1, ArrayList var2, ArrayList var3, int var4) {
    if (var0 < var1) {
      int var5 = var0 + (var1 - var0) / 2;
      if (++var4 < n) {
        a(var0, var5, var2, var3, var4);
        a(var5 + 1, var1, var2, var3, var4);
      }

      a(var0, var5, var1, var2, var3);
    }
  }

  private static void a(int var0, int var1, int var2, ArrayList var3, ArrayList var4) {
    int var5 = var0;
    int var6 = var1 + 1;

    for (int var7 = var0; var7 <= var2; var7++) {
      var4.set(var7, var3.get(var7));
    }

    while (var5 <= var1 && var6 <= var2) {
      ILongDecrypter var8;
      if (((ILongDecrypter)var4.get(var5)).equals((ILongDecrypter)var4.get(var6))) {
        var8 = (ILongDecrypter)var4.get(var5++);
      } else {
        var8 = (ILongDecrypter)var4.get(var6++);
      }

      var3.set(var0, var8);
      var0++;
    }

    while (var5 <= var1) {
      var3.set(var0, var4.get(var5));
      var0++;
      var5++;
    }
  }

  static {
    long var0 = 1L;

    for (int i = 0; i < 64; i++) {
      someLongNumberPool[i] = var0;
      var0 <<= 1;
    }

    o = new Object();
    lookupClasses = new Vector();
    f = new ArrayList();
    a0();
    i = f.size();
    c();
  }

  private static void a0() {
    mutableNumberPool = new int[]{
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
    f.add(new LongDecrypter1(-6744423634385984943L));
    f.add(new LongDecrypter1(-6706934200393210049L));
    f.add(new LongDecrypter1(6990400485665549342L));
    f.add(new LongDecrypter1(8478105918229553425L));
    f.add(new LongDecrypter1(-4523791493794833373L));
    f.add(new LongDecrypter1(6956550957728802162L));
    f.add(new LongDecrypter1(-6378052640071885957L));
    f.add(new LongDecrypter1(-8115039236204621966L));
    f.add(new LongDecrypter1(-3404969271164910260L));
    f.add(new LongDecrypter1(-6837460718211466928L));
    f.add(new LongDecrypter1(909643428573496924L));
    f.add(new LongDecrypter1(-2853620746374076905L));
    f.add(new LongDecrypter1(7558632103745708878L));
    f.add(new LongDecrypter1(-4451995920286813933L));
    f.add(new LongDecrypter1(2987343977142760299L));
    f.add(new LongDecrypter1(8774831055784385353L));
    f.add(new LongDecrypter1(-2491703159784603645L));
    f.add(new LongDecrypter1(-3825761391279259103L));
    f.add(new LongDecrypter1(-8103271119208899000L));
    f.add(new LongDecrypter1(-3751748464649851844L));
    f.add(new LongDecrypter1(4655894350249038690L));
    f.add(new LongDecrypter1(2299116995506572841L));
    f.add(new LongDecrypter1(-4248253892470355672L));
    f.add(new LongDecrypter1(673339327068868620L));
    f.add(new LongDecrypter1(928988291849119595L));
    f.add(new LongDecrypter1(-4494707613054168802L));
    f.add(new LongDecrypter1(8219881982464703334L));
    f.add(new LongDecrypter1(1592580442844519725L));
    f.add(new LongDecrypter1(6521590094206246995L));
    f.add(new LongDecrypter1(-7862763716460523474L));
    f.add(new LongDecrypter1(-7180250963500505561L));
    f.add(new LongDecrypter1(-1990481056537250245L));
    f.add(new LongDecrypter1(-4259418252620178920L));
    f.add(new LongDecrypter1(115230540288189211L));
    f.add(new LongDecrypter1(-260614310560079233L));
    f.add(new LongDecrypter1(-4560237432271780805L));
    f.add(new LongDecrypter1(-5488233296441099736L));
    f.add(new LongDecrypter1(-6602289711462896865L));
    f.add(new LongDecrypter1(6238345315009356580L));
    f.add(new LongDecrypter1(4887668945434071164L));
    f.add(new LongDecrypter1(-7080207893442515422L));
    f.add(new LongDecrypter1(4442284130421016109L));
    f.add(new LongDecrypter1(8540532653575655206L));
    f.add(new LongDecrypter1(-4153213651530454446L));
    f.add(new LongDecrypter1(-4650655848889566882L));
    f.add(new LongDecrypter1(8172887638636256619L));
    f.add(new LongDecrypter1(285257623463465760L));
    f.add(new LongDecrypter1(-2212661351872423105L));
    f.add(new LongDecrypter1(-8955014614648852638L));
    f.add(new LongDecrypter1(7685371999257522432L));
    f.add(new LongDecrypter1(-7480559610018095299L));
    f.add(new LongDecrypter1(7645975422665949738L));
    f.add(new LongDecrypter1(-8502689991448483739L));
    f.add(new LongDecrypter1(4027701581277325167L));
    f.add(new LongDecrypter1(1632673887745594740L));
    f.add(new LongDecrypter1(-6478213330387212258L));
    f.add(new LongDecrypter1(-5284458860625299893L));
    f.add(new LongDecrypter1(4188699928120191279L));
    f.add(new LongDecrypter1(1672758662401941534L));
    f.add(new LongDecrypter1(5269287403847926315L));
    f.add(new LongDecrypter1(6948122180030299767L));
    f.add(new LongDecrypter1(7605450297947617089L));
    f.add(new LongDecrypter1(-8309140045178447544L));
    f.add(new LongDecrypter1(5320132290067212846L));
    f.add(new LongDecrypter1(-4793472161571137674L));
    f.add(new LongDecrypter1(335755845515907412L));
    f.add(new LongDecrypter1(8229858481322148701L));
    f.add(new LongDecrypter1(-481794707622686634L));
    f.add(new LongDecrypter1(-6395100604994942360L));
    f.add(new LongDecrypter1(-6119572320851432877L));
    f.add(new LongDecrypter1(2184643316460692798L));
    f.add(new LongDecrypter1(-8981503674350135997L));
    f.add(new LongDecrypter1(-7656732516946638003L));
    f.add(new LongDecrypter1(-5635213181667554701L));
    f.add(new LongDecrypter1(-1151223719834730898L));
    f.add(new LongDecrypter1(-611382292272610294L));
    f.add(new LongDecrypter1(-7885244854120664817L));
    f.add(new LongDecrypter1(1657074236485762147L));
    f.add(new LongDecrypter1(-5902840883215474855L));
    f.add(new LongDecrypter1(6615869915130831983L));
    f.add(new LongDecrypter1(3088267513636873593L));
    f.add(new LongDecrypter1(-3461658328266714253L));
    f.add(new LongDecrypter1(-6629250593412551926L));
    f.add(new LongDecrypter1(1996215071285581834L));
    f.add(new LongDecrypter1(-5002744937498769605L));
    f.add(new LongDecrypter1(-6355238420635454613L));
    f.add(new LongDecrypter1(-5181722811629877386L));
    f.add(new LongDecrypter1(-6883115128543829165L));
    f.add(new LongDecrypter1(6088188338395302712L));
    f.add(new LongDecrypter1(2611888656600445821L));
    f.add(new LongDecrypter1(-4646520290036149891L));
    f.add(new LongDecrypter1(-4378219892565776629L));
    f.add(new LongDecrypter1(8976129125876155436L));
    f.add(new LongDecrypter1(-8363523088435887098L));
    f.add(new LongDecrypter1(86840831297761602L));
    f.add(new LongDecrypter1(7787591308013543461L));
    f.add(new LongDecrypter1(3469387858000844866L));
    f.add(new LongDecrypter1(-5233032057401438375L));
    f.add(new LongDecrypter1(-9118749419407424250L));
    f.add(new LongDecrypter1(-8500163652186440446L));
    f.add(new LongDecrypter1(3757520509916313379L));
    f.add(new LongDecrypter1(-4116244975720934845L));
    f.add(new LongDecrypter1(-6178501944032822413L));
    f.add(new LongDecrypter1(-9153356831469675727L));
    f.add(new LongDecrypter1(-1063398336654163675L));
    f.add(new LongDecrypter1(770696137606370564L));
    f.add(new LongDecrypter1(-8186032946492650175L));
    f.add(new LongDecrypter1(1593212751750682921L));
    f.add(new LongDecrypter1(-5956464615920267193L));
    f.add(new LongDecrypter1(-8015458933371737043L));
    f.add(new LongDecrypter1(-5554639710744978350L));
    f.add(new LongDecrypter1(3650944509352886554L));
    f.add(new LongDecrypter1(-3169510854341612216L));
    f.add(new LongDecrypter1(-1828008516145470950L));
    f.add(new LongDecrypter1(-6789118416236927168L));
    f.add(new LongDecrypter1(-4762700133102726812L));
    f.add(new LongDecrypter1(8878911884416891471L));
    f.add(new LongDecrypter1(8664621867198439947L));
    f.add(new LongDecrypter1(8916388788512143153L));
    f.add(new LongDecrypter1(4412593360637248002L));
    f.add(new LongDecrypter1(6927240979754877261L));
    f.add(new LongDecrypter1(-1216636158766706064L));
    f.add(new LongDecrypter1(-7497317701947733992L));
    f.add(new LongDecrypter1(5868253207908670304L));
    f.add(new LongDecrypter1(8271568981229458725L));
    f.add(new LongDecrypter1(1582435105153641742L));
    f.add(new LongDecrypter1(1003193982455307812L));
    f.add(new LongDecrypter1(-3744940415362494952L));
    f.add(new LongDecrypter1(-8057241827569097198L));
    f.add(new LongDecrypter1(-8160938211100963327L));
    f.add(new LongDecrypter1(5426810931933071203L));
    f.add(new LongDecrypter1(3484536533526790993L));
    f.add(new LongDecrypter1(8909305946017264730L));
    f.add(new LongDecrypter1(-607288752669276128L));
    f.add(new LongDecrypter1(-1500707136245063922L));
    f.add(new LongDecrypter1(-1112894094489036189L));
    f.add(new LongDecrypter1(-2186260659538304726L));
    f.add(new LongDecrypter1(8497316323465943560L));
    f.add(new LongDecrypter1(2378738370292703073L));
    f.add(new LongDecrypter1(4256453459033539335L));
    f.add(new LongDecrypter1(7399179070313343094L));
    f.add(new LongDecrypter1(4994067841559076140L));
    f.add(new LongDecrypter1(565607164416017254L));
    f.add(new LongDecrypter1(-4047108070709861516L));
    f.add(new LongDecrypter1(-1857416293356308444L));
    f.add(new LongDecrypter1(4048018357712373000L));
    f.add(new LongDecrypter1(5197029575525773668L));
    f.add(new LongDecrypter1(-3547717426064585430L));
    f.add(new LongDecrypter1(4760024407670384218L));
    f.add(new LongDecrypter1(313069990748533254L));
    f.add(new LongDecrypter1(-5348539643686660003L));
    f.add(new LongDecrypter1(-7272107145001041857L));
    f.add(new LongDecrypter1(-4615823146970798776L));
    f.add(new LongDecrypter1(6802957569116356865L));
    f.add(new LongDecrypter1(109198422163880778L));
    f.add(new LongDecrypter1(719550753215290476L));
    f.add(new LongDecrypter1(663070648437475965L));
    f.add(new LongDecrypter1(-7982366021878349477L));
    f.add(new LongDecrypter1(-3154350453178068187L));
    f.add(new LongDecrypter1(-7079747071937835226L));
    f.add(new LongDecrypter1(-4852688329732284852L));
    mutableNumberPool = (int[]) mutableNumberPool.clone();
    f.add(new LongDecrypter1(6924729867076140072L));
    f.add(new LongDecrypter1(-1319055387195296970L));
    f.add(new LongDecrypter1(1110718098836545406L));
    f.add(new LongDecrypter1(-5184487272089418201L));
    f.add(new LongDecrypter1(-1209929019013289676L));
    f.add(new LongDecrypter1(-3934943933646132417L));
    f.add(new LongDecrypter1(6612345466967366939L));
    f.add(new LongDecrypter1(-7983784829243525038L));
    f.add(new LongDecrypter1(-3465473421090667983L));
    f.add(new LongDecrypter1(4955069875975476330L));
    f.add(new LongDecrypter1(1222738085821493094L));
    f.add(new LongDecrypter1(7274631535676754294L));
    f.add(new LongDecrypter1(3605167428510276924L));
    f.add(new LongDecrypter1(7321401848686268006L));
    f.add(new LongDecrypter1(-8537181269155846398L));
    f.add(new LongDecrypter1(7036010071487345011L));
    f.add(new LongDecrypter1(6469190484441503783L));
    f.add(new LongDecrypter1(358554658493788272L));
    f.add(new LongDecrypter1(4801186158922092356L));
    f.add(new LongDecrypter1(2025941902864584272L));
    f.add(new LongDecrypter1(5046251421333692002L));
    f.add(new LongDecrypter1(4346213119371418670L));
    f.add(new LongDecrypter1(1952490212279718209L));
    f.add(new LongDecrypter1(388904733011491916L));
    f.add(new LongDecrypter1(8032609794616146291L));
    f.add(new LongDecrypter1(-5947827916444804545L));
    f.add(new LongDecrypter1(-1529830667070193801L));
    f.add(new LongDecrypter1(6425974338071218780L));
    f.add(new LongDecrypter1(-3889257601660206246L));
    f.add(new LongDecrypter1(3534689640069068320L));
    f.add(new LongDecrypter1(-6044722655579239339L));
    f.add(new LongDecrypter1(4849810454304746038L));
    f.add(new LongDecrypter1(-5622382021455524496L));
    f.add(new LongDecrypter1(-2019625191226615731L));
    f.add(new LongDecrypter1(-4709691165499111106L));
    f.add(new LongDecrypter1(1792484731624864873L));
    f.add(new LongDecrypter1(7281842097693247804L));
    f.add(new LongDecrypter1(-60290756861186714L));
    f.add(new LongDecrypter1(6474293750210062665L));
    f.add(new LongDecrypter1(1393182141334933255L));
    f.add(new LongDecrypter1(-4414579491870249421L));
    f.add(new LongDecrypter1(-4841279960438382325L));
    f.add(new LongDecrypter1(2662755756897883207L));
    f.add(new LongDecrypter1(-333062117895070857L));
    f.add(new LongDecrypter1(7651854756009155114L));
    f.add(new LongDecrypter1(-6649917936615431635L));
    f.add(new LongDecrypter1(2215331531862059131L));
    f.add(new LongDecrypter1(-4626100401364144631L));
    f.add(new LongDecrypter1(8597788376328339537L));
    f.add(new LongDecrypter1(-5419944576110703776L));
    f.add(new LongDecrypter1(1647514364380703233L));
    f.add(new LongDecrypter1(4913700706030770261L));
    f.add(new LongDecrypter1(2972229743455703418L));
    f.add(new LongDecrypter1(-7097384657830585849L));
    f.add(new LongDecrypter1(5866054288827446814L));
    f.add(new LongDecrypter1(2635971902479150421L));
    f.add(new LongDecrypter1(-1234291004083169172L));
    f.add(new LongDecrypter1(-6296365828022667262L));
    f.add(new LongDecrypter1(-6198154523778839226L));
    f.add(new LongDecrypter1(7280882661630946318L));
    f.add(new LongDecrypter1(-1163246228181812916L));
    f.add(new LongDecrypter1(8058917293246814304L));
    f.add(new LongDecrypter1(2555097195074984218L));
    f.add(new LongDecrypter1(5238458381956505444L));
    f.add(new LongDecrypter1(-5274531756641706705L));
    f.add(new LongDecrypter1(-4778378953595314612L));
    f.add(new LongDecrypter1(5443240005591331681L));
    f.add(new LongDecrypter1(-1922697014945020873L));
    f.add(new LongDecrypter1(-758164801927635189L));
    f.add(new LongDecrypter1(-3750102876210478271L));
    f.add(new LongDecrypter1(922384580518357556L));
    f.add(new LongDecrypter1(-5346287844412991411L));
    f.add(new LongDecrypter1(6937062962655216499L));
    f.add(new LongDecrypter1(5479188277678227209L));
    f.add(new LongDecrypter1(-6355320449421308357L));
    f.add(new LongDecrypter1(773272052063831344L));
    f.add(new LongDecrypter1(2250951039312121397L));
    f.add(new LongDecrypter1(7509361718814825052L));
    f.add(new LongDecrypter1(1064803118799081080L));
    f.add(new LongDecrypter1(-8090526503937156583L));
    f.add(new LongDecrypter1(-4660199197585511367L));
    f.add(new LongDecrypter1(4990315584293803368L));
    f.add(new LongDecrypter1(1929101553301683300L));
    f.add(new LongDecrypter1(3706831602997022299L));
    f.add(new LongDecrypter1(3244945072854826617L));
    f.add(new LongDecrypter1(-6721406422293792749L));
    f.add(new LongDecrypter1(-376202964417428685L));
    f.add(new LongDecrypter1(3020618294975159574L));
    f.add(new LongDecrypter1(2524768609197567760L));
    mutableNumberPool = (int[]) mutableNumberPool.clone();
    f.add(new LongDecrypter1(-5106164513165795732L));
    f.add(new LongDecrypter1(-673701620128658570L));
    f.add(new LongDecrypter1(3032839098405056378L));
    f.add(new LongDecrypter1(8157567941448647257L));
    f.add(new LongDecrypter1(2322221287690188288L));
    f.add(new LongDecrypter1(-933042621612592580L));
    f.add(new LongDecrypter1(8699774343065680999L));
    f.add(new LongDecrypter1(2037709077370185054L));
    f.add(new LongDecrypter1(-25683838325834428L));
    f.add(new LongDecrypter1(2092240939937505885L));
    f.add(new LongDecrypter1(-1775291314990744973L));
    f.add(new LongDecrypter1(-1636075790375377310L));
    f.add(new LongDecrypter1(-1936241699616714117L));
    f.add(new LongDecrypter1(5777575772619753739L));
    f.add(new LongDecrypter1(4442815237220284737L));
    f.add(new LongDecrypter1(1360860040086278686L));
    f.add(new LongDecrypter1(3844824520896299857L));
    f.add(new LongDecrypter1(-8075846110590462860L));
    f.add(new LongDecrypter1(-6750562378065394424L));
    f.add(new LongDecrypter1(-2071856796292556699L));
    f.add(new LongDecrypter1(7645975440382689850L));
    f.add(new LongDecrypter1(1973643837185670230L));
    f.add(new LongDecrypter1(-6596323918647719115L));
    f.add(new LongDecrypter1(-5505711763130324890L));
    f.add(new LongDecrypter1(-590881294597069820L));
    f.add(new LongDecrypter1(5149094331846857342L));
    f.add(new LongDecrypter1(-6649986424440623541L));
    f.add(new LongDecrypter1(-475936916104918188L));
    f.add(new LongDecrypter1(-5164782238865553122L));
    f.add(new LongDecrypter1(8366919996635922256L));
    f.add(new LongDecrypter1(-3160648661163137486L));
    f.add(new LongDecrypter1(-880428123426138517L));
    f.add(new LongDecrypter1(-4425756912056562425L));
    f.add(new LongDecrypter1(-4887734184666877140L));
    f.add(new LongDecrypter1(6240037117283760480L));
    f.add(new LongDecrypter1(1690480913210458466L));
    f.add(new LongDecrypter1(-3491202511124849912L));
    f.add(new LongDecrypter1(-8799598494005851568L));
    f.add(new LongDecrypter1(7322312843080944694L));
    f.add(new LongDecrypter1(-6842343091803594885L));
    f.add(new LongDecrypter1(4600781611143661320L));
    f.add(new LongDecrypter1(2501933747265243007L));
    f.add(new LongDecrypter1(157513467097159432L));
    f.add(new LongDecrypter1(-51397667266540032L));
    f.add(new LongDecrypter1(-5197404301377746114L));
    f.add(new LongDecrypter1(5561856049065377315L));
    f.add(new LongDecrypter1(6847457923712613988L));
    f.add(new LongDecrypter1(690810815984985962L));
    f.add(new LongDecrypter1(6910462901404226130L));
    f.add(new LongDecrypter1(9066171932773458292L));
    f.add(new LongDecrypter1(2979620315348532580L));
    f.add(new LongDecrypter1(966601938649563477L));
    f.add(new LongDecrypter1(-1979829244037265369L));
    f.add(new LongDecrypter1(-6138508966444145039L));
    f.add(new LongDecrypter1(7285017837801602088L));
    f.add(new LongDecrypter1(-5337906342982701542L));
    f.add(new LongDecrypter1(5698217665490279274L));
    f.add(new LongDecrypter1(-6279771978459474340L));
    f.add(new LongDecrypter1(306438502956140149L));
    f.add(new LongDecrypter1(6022601678053684299L));
    f.add(new LongDecrypter1(-6247038219811084434L));
    f.add(new LongDecrypter1(-3485642966629178362L));
    f.add(new LongDecrypter1(-280204998506807255L));
    f.add(new LongDecrypter1(811953170445661739L));
    f.add(new LongDecrypter1(6676356884277843262L));
    f.add(new LongDecrypter1(4203274232037917986L));
    f.add(new LongDecrypter1(6218698579016947227L));
    f.add(new LongDecrypter1(9062922882051374362L));
    f.add(new LongDecrypter1(-7392999613859714256L));
    f.add(new LongDecrypter1(1951270258536690178L));
    f.add(new LongDecrypter1(6475368503852093309L));
    f.add(new LongDecrypter1(-6094943361180325249L));
    f.add(new LongDecrypter1(-8238626769688455661L));
    f.add(new LongDecrypter1(-8491508749498464741L));
    f.add(new LongDecrypter1(-1755966378406569721L));
    f.add(new LongDecrypter1(8524881805879043193L));
    f.add(new LongDecrypter1(-628980067508333726L));
    f.add(new LongDecrypter1(-7553595808090830267L));
    f.add(new LongDecrypter1(-3401088427898417385L));
    f.add(new LongDecrypter1(8148495414709501776L));
    f.add(new LongDecrypter1(2391241009525101111L));
    f.add(new LongDecrypter1(-846777915758294233L));
    f.add(new LongDecrypter1(-757066702853567984L));
    f.add(new LongDecrypter1(7858483658340044671L));
    f.add(new LongDecrypter1(-8765913414195489930L));
    f.add(new LongDecrypter1(-1071410693439370419L));
    f.add(new LongDecrypter1(6937867782310580346L));
    f.add(new LongDecrypter1(-4951001095487041465L));
    f.add(new LongDecrypter1(7185266059361506062L));
    f.add(new LongDecrypter1(2748102153912659494L));
    f.add(new LongDecrypter1(4738100879865434414L));
    f.add(new LongDecrypter1(3145300095240542486L));
    f.add(new LongDecrypter1(-6535859322297191350L));
    f.add(new LongDecrypter1(-4507200759147770011L));
    f.add(new LongDecrypter1(-1740123166599572719L));
    f.add(new LongDecrypter1(-9219816827940516490L));
    f.add(new LongDecrypter1(591935923207441183L));
    f.add(new LongDecrypter1(-6931212079319678924L));
    f.add(new LongDecrypter1(1927951755889431824L));
    f.add(new LongDecrypter1(1018125983805817356L));
    f.add(new LongDecrypter1(-1713219551072842643L));
    f.add(new LongDecrypter1(-1993225270065169106L));
    f.add(new LongDecrypter1(1497482565846420089L));
    f.add(new LongDecrypter1(-8419134277282359775L));
    f.add(new LongDecrypter1(1016119785387431222L));
    f.add(new LongDecrypter1(-7860660830204724684L));
    f.add(new LongDecrypter1(8223057368263223413L));
    f.add(new LongDecrypter1(-3672232568321311139L));
    f.add(new LongDecrypter1(-8928735346180880288L));
    f.add(new LongDecrypter1(-5703536978811697351L));
    f.add(new LongDecrypter1(-4373616876358252673L));
    f.add(new LongDecrypter1(5496240696206782572L));
    f.add(new LongDecrypter1(8911596651907001674L));
    f.add(new LongDecrypter1(-8466133811519483080L));
    f.add(new LongDecrypter1(-7036095350088556279L));
    f.add(new LongDecrypter1(-6439300964726081222L));
    f.add(new LongDecrypter1(7559953241327815772L));
    f.add(new LongDecrypter1(8509983995108710244L));
    f.add(new LongDecrypter1(8126543840463541282L));
    f.add(new LongDecrypter1(4679530916572832059L));
    f.add(new LongDecrypter1(3315415731403675506L));
    f.add(new LongDecrypter1(6563689203004965384L));
    f.add(new LongDecrypter1(-6399952368373559232L));
    f.add(new LongDecrypter1(-6970545074926306033L));
    f.add(new LongDecrypter1(9206226304900513604L));
    f.add(new LongDecrypter1(4183589437500349814L));
    f.add(new LongDecrypter1(704930295637337170L));
    f.add(new LongDecrypter1(-6291063742149989843L));
    f.add(new LongDecrypter1(-6861793193461154999L));
    f.add(new LongDecrypter1(-5213481917054572740L));
    f.add(new LongDecrypter1(-6202798052391301836L));
    f.add(new LongDecrypter1(5227330657153389860L));
    f.add(new LongDecrypter1(7861980581239952747L));
    f.add(new LongDecrypter1(-6411638694933490114L));
    f.add(new LongDecrypter1(-7218582584421862135L));
    f.add(new LongDecrypter1(-2456101936301468395L));
    f.add(new LongDecrypter1(7309193604905058320L));
    f.add(new LongDecrypter1(-1169766170650380281L));
    f.add(new LongDecrypter1(-5149765774756432886L));
    f.add(new LongDecrypter1(3619941783783821365L));
    f.add(new LongDecrypter1(4755953108808997225L));
    f.add(new LongDecrypter1(-5898742921671940225L));
    f.add(new LongDecrypter1(-1513806074914163381L));
    f.add(new LongDecrypter1(213638593212357751L));
    f.add(new LongDecrypter1(1941308327608399155L));
    f.add(new LongDecrypter1(-8652072385151148020L));
    f.add(new LongDecrypter1(-5518343174315310225L));
    f.add(new LongDecrypter1(6668391554757700701L));
    f.add(new LongDecrypter1(3378303108682973236L));
    f.add(new LongDecrypter1(153551912349589258L));
    f.add(new LongDecrypter1(-7174519294373221017L));
    f.add(new LongDecrypter1(5576411236907470600L));
    f.add(new LongDecrypter1(3922072894231107391L));
    f.add(new LongDecrypter1(3084936538615937343L));
    f.add(new LongDecrypter1(-365481539812195825L));
    f.add(new LongDecrypter1(-9074392047666671583L));
    f.add(new LongDecrypter1(-5252333847380776371L));
    f.add(new LongDecrypter1(-8392327288880097969L));
    f.add(new LongDecrypter1(-3749904315140114141L));
    f.add(new LongDecrypter1(917548018462944056L));
    f.add(new LongDecrypter1(4410000888652955656L));
    mutableNumberPool = (int[]) mutableNumberPool.clone();
    f.add(new LongDecrypter1(4309931546848104734L));
    f.add(new LongDecrypter1(7825918609744072054L));
    f.add(new LongDecrypter1(2034815114218434608L));
    f.add(new LongDecrypter1(6815058800263487518L));
    f.add(new LongDecrypter1(-6043575486452002712L));
    f.add(new LongDecrypter1(-4170086999604523937L));
    f.add(new LongDecrypter1(2831257470140081013L));
    f.add(new LongDecrypter1(7073463272410348565L));
    f.add(new LongDecrypter1(-6837280645850915462L));
    f.add(new LongDecrypter1(3780232701099159317L));
    f.add(new LongDecrypter1(7827162877968932208L));
    f.add(new LongDecrypter1(-8876862564686567611L));
    f.add(new LongDecrypter1(1816290325167492872L));
    f.add(new LongDecrypter1(6084032547809917278L));
    f.add(new LongDecrypter1(-2004026282083097840L));
    f.add(new LongDecrypter1(5681691551577807424L));
    f.add(new LongDecrypter1(1362384365585262897L));
    f.add(new LongDecrypter1(8254130692795050797L));
    f.add(new LongDecrypter1(95854038055961911L));
    f.add(new LongDecrypter1(7584561608549564522L));
    f.add(new LongDecrypter1(2225803302692197L));
    f.add(new LongDecrypter1(-7925858458233475061L));
    f.add(new LongDecrypter1(-783063673896965100L));
    f.add(new LongDecrypter1(-6474177126950886066L));
    f.add(new LongDecrypter1(1424906516450865987L));
    f.add(new LongDecrypter1(8994706141977867292L));
    f.add(new LongDecrypter1(-5427762163764242642L));
    f.add(new LongDecrypter1(752633996393504890L));
    f.add(new LongDecrypter1(-3821773867277842895L));
    f.add(new LongDecrypter1(-5044365075408941478L));
    f.add(new LongDecrypter1(4334696141216550490L));
    f.add(new LongDecrypter1(5223622137639054093L));
    f.add(new LongDecrypter1(-3747734116506103956L));
    f.add(new LongDecrypter1(3476627681176321150L));
    f.add(new LongDecrypter1(6701728281633932876L));
    f.add(new LongDecrypter1(7879316057935635805L));
    f.add(new LongDecrypter1(-1961540790445216207L));
    f.add(new LongDecrypter1(5609925979471008083L));
    f.add(new LongDecrypter1(2758595237753181999L));
    f.add(new LongDecrypter1(3828868950989022787L));
    f.add(new LongDecrypter1(7554970856115997762L));
    f.add(new LongDecrypter1(-1674289599311139799L));
    f.add(new LongDecrypter1(-5882541554291952257L));
    f.add(new LongDecrypter1(-5970517969145226666L));
    f.add(new LongDecrypter1(5694327872563891786L));
    f.add(new LongDecrypter1(-9075081430626130678L));
    f.add(new LongDecrypter1(510363649096219245L));
    f.add(new LongDecrypter1(2932043914224158007L));
    f.add(new LongDecrypter1(-2103310732740451281L));
    f.add(new LongDecrypter1(9143091788327051278L));
    f.add(new LongDecrypter1(-8190331378858654425L));
    f.add(new LongDecrypter1(-6484515748255825118L));
    f.add(new LongDecrypter1(-989102733826402503L));
    f.add(new LongDecrypter1(-2805589460348619455L));
    f.add(new LongDecrypter1(4952624266317925170L));
    f.add(new LongDecrypter1(-7866602387523759598L));
    f.add(new LongDecrypter1(-2838535743579155169L));
    f.add(new LongDecrypter1(9130337089540320078L));
    f.add(new LongDecrypter1(-6155941245566701791L));
    f.add(new LongDecrypter1(4369077573250708827L));
    f.add(new LongDecrypter1(-3403922283335250125L));
    f.add(new LongDecrypter1(-5228145801860091857L));
    f.add(new LongDecrypter1(5611646657723339879L));
    f.add(new LongDecrypter1(-3727659461417825161L));
    f.add(new LongDecrypter1(-8098452576981511931L));
    f.add(new LongDecrypter1(-5840023793533036775L));
    f.add(new LongDecrypter1(305061427025647218L));
    f.add(new LongDecrypter1(1554589124966828544L));
    f.add(new LongDecrypter1(-995902213687802079L));
    f.add(new LongDecrypter1(1661298148894725234L));
    f.add(new LongDecrypter1(-1171190151657088490L));
    f.add(new LongDecrypter1(-1911122725552527299L));
    f.add(new LongDecrypter1(-4166212015522712752L));
    f.add(new LongDecrypter1(1307637690161925921L));
    f.add(new LongDecrypter1(-4807711348563110904L));
    f.add(new LongDecrypter1(-7911655230983048329L));
    f.add(new LongDecrypter1(6183208992543283055L));
    f.add(new LongDecrypter1(-1274010569969155473L));
    f.add(new LongDecrypter1(-248690450166941417L));
    f.add(new LongDecrypter1(438974322021211749L));
    f.add(new LongDecrypter1(-8521068021660184464L));
    f.add(new LongDecrypter1(-3156900595960784616L));
    f.add(new LongDecrypter1(946611373396269568L));
    f.add(new LongDecrypter1(-3330553286879175391L));
    f.add(new LongDecrypter1(3003257089522996293L));
    f.add(new LongDecrypter1(5539589064221234295L));
    f.add(new LongDecrypter1(-6147579707562708687L));
    f.add(new LongDecrypter1(-6271026328840650696L));
    f.add(new LongDecrypter1(8038276827517709911L));
    f.add(new LongDecrypter1(-6398103833070748828L));
    f.add(new LongDecrypter1(-3657318447125409469L));
    f.add(new LongDecrypter1(-8383621622363403705L));
    f.add(new LongDecrypter1(8216605443336037219L));
    f.add(new LongDecrypter1(-8713102081639797436L));
    f.add(new LongDecrypter1(-7543937831586873292L));
    f.add(new LongDecrypter1(-8993887115287846087L));
    f.add(new LongDecrypter1(857243566681711432L));
    f.add(new LongDecrypter1(6877473343732551430L));
    f.add(new LongDecrypter1(-6442566549481596084L));
    f.add(new LongDecrypter1(8346392322474908715L));
    f.add(new LongDecrypter1(8950600426892419611L));
    f.add(new LongDecrypter1(-2251462301258663597L));
    f.add(new LongDecrypter1(8235262215580202595L));
    f.add(new LongDecrypter1(-2181622447026851046L));
    f.add(new LongDecrypter1(-3920438834459637895L));
    f.add(new LongDecrypter1(6962082836595194640L));
    f.add(new LongDecrypter1(-4148519272451968693L));
    f.add(new LongDecrypter1(-161146386391559860L));
    f.add(new LongDecrypter1(-2665459711608506848L));
    f.add(new LongDecrypter1(3709258874196377632L));
    f.add(new LongDecrypter1(6039434875928637786L));
    f.add(new LongDecrypter1(-2178029681318653626L));
    f.add(new LongDecrypter1(-744779604243454667L));
    f.add(new LongDecrypter1(-9024143963715833852L));
    f.add(new LongDecrypter1(-2691682731458806928L));
    f.add(new LongDecrypter1(7018807265652199746L));
    mutableNumberPool = (int[]) mutableNumberPool.clone();
    f.add(new LongDecrypter1(-919413858743417798L));
    f.add(new LongDecrypter1(-1774623754234802882L));
    f.add(new LongDecrypter1(-8339418576012490680L));
    f.add(new LongDecrypter1(-7513479833408246487L));
    f.add(new LongDecrypter1(-2314818037513073106L));
    f.add(new LongDecrypter1(-9074274855793584370L));
    f.add(new LongDecrypter1(3277659344386098540L));
    f.add(new LongDecrypter1(-8045473141863789512L));
    f.add(new LongDecrypter1(-7970299096669443799L));
    f.add(new LongDecrypter1(1034754903083837051L));
    f.add(new LongDecrypter1(3100201980261012324L));
    f.add(new LongDecrypter1(-3701670125437049291L));
    f.add(new LongDecrypter1(5653162943558428452L));
    f.add(new LongDecrypter1(-1625458280817133288L));
    f.add(new LongDecrypter1(9168198672079513207L));
    f.add(new LongDecrypter1(1756840750465306970L));
    f.add(new LongDecrypter1(-3563527016006297273L));
    f.add(new LongDecrypter1(3124357265138734874L));
    f.add(new LongDecrypter1(-766134785560780261L));
    f.add(new LongDecrypter1(-3590677551555116172L));
    f.add(new LongDecrypter1(-1580854947335065240L));
    f.add(new LongDecrypter1(-3679035563956529320L));
    f.add(new LongDecrypter1(-4131972852617518234L));
    f.add(new LongDecrypter1(-1916171663225653717L));
    f.add(new LongDecrypter1(-8888138375515087806L));
    f.add(new LongDecrypter1(837547871646678133L));
    f.add(new LongDecrypter1(8390388916778336276L));
    f.add(new LongDecrypter1(5285180317943538453L));
    f.add(new LongDecrypter1(-9195622821895535786L));
    f.add(new LongDecrypter1(-4568494496855487641L));
    f.add(new LongDecrypter1(1723762583039395626L));
    f.add(new LongDecrypter1(-3656784539222589676L));
    f.add(new LongDecrypter1(-5045168601700388826L));
    f.add(new LongDecrypter1(-2029989707988253900L));
    f.add(new LongDecrypter1(-5590402094919585524L));
    f.add(new LongDecrypter1(4465196013497954929L));
    f.add(new LongDecrypter1(7131193391680124420L));
    f.add(new LongDecrypter1(-1457143371037150647L));
    f.add(new LongDecrypter1(2025195411394300269L));
    f.add(new LongDecrypter1(-2392720093380125878L));
    f.add(new LongDecrypter1(-9180298646049917071L));
    f.add(new LongDecrypter1(-6829077588024813969L));
    f.add(new LongDecrypter1(-3590114192596026103L));
    f.add(new LongDecrypter1(4177899441345042019L));
    f.add(new LongDecrypter1(1932025508841098613L));
    f.add(new LongDecrypter1(4374414213058128253L));
    f.add(new LongDecrypter1(-2635133127341295326L));
    f.add(new LongDecrypter1(-1212775154779584214L));
    f.add(new LongDecrypter1(-2077230865184567803L));
    f.add(new LongDecrypter1(8532627771998088723L));
    f.add(new LongDecrypter1(-1918301575936345551L));
    f.add(new LongDecrypter1(-6181119479344910834L));
    f.add(new LongDecrypter1(7607659780887766874L));
    f.add(new LongDecrypter1(8841419320954119994L));
    f.add(new LongDecrypter1(-3386952276780610202L));
    f.add(new LongDecrypter1(-7265218758822181265L));
    f.add(new LongDecrypter1(-6885763395958973390L));
    f.add(new LongDecrypter1(7175179636323001898L));
    f.add(new LongDecrypter1(965306621268280864L));
    f.add(new LongDecrypter1(-8964937495528998351L));
    f.add(new LongDecrypter1(7945823546398330117L));
    f.add(new LongDecrypter1(6582853383060287522L));
    f.add(new LongDecrypter1(-3013505789382482932L));
    f.add(new LongDecrypter1(6345532957235816740L));
    f.add(new LongDecrypter1(7717839717785972351L));
    f.add(new LongDecrypter1(303352763970566936L));
    f.add(new LongDecrypter1(3993071653855238260L));
    f.add(new LongDecrypter1(6355371785498034955L));
    f.add(new LongDecrypter1(4409817751224932099L));
    f.add(new LongDecrypter1(-5025307611986570656L));
    f.add(new LongDecrypter1(-516646206414250491L));
    f.add(new LongDecrypter1(-207387019995361789L));
    f.add(new LongDecrypter1(-5930061197256787957L));
    f.add(new LongDecrypter1(4664346185322264172L));
    f.add(new LongDecrypter1(8975714574788740655L));
    f.add(new LongDecrypter1(9074650266398893630L));
    f.add(new LongDecrypter1(4670921149196815655L));
    f.add(new LongDecrypter1(2065487589269618250L));
    f.add(new LongDecrypter1(8002737148078177618L));
    f.add(new LongDecrypter1(7613899854096587131L));
    f.add(new LongDecrypter1(8363971138228398118L));
    f.add(new LongDecrypter1(-336398731228566917L));
    f.add(new LongDecrypter1(-6067865635786431478L));
    f.add(new LongDecrypter1(-537350056175380937L));
    f.add(new LongDecrypter1(-821403342938294173L));
    f.add(new LongDecrypter1(1388933875687454829L));
    f.add(new LongDecrypter1(-1811447339585681064L));
    f.add(new LongDecrypter1(5723306975138455595L));
    f.add(new LongDecrypter1(-917198550118498522L));
    f.add(new LongDecrypter1(-2067564707638383745L));
    f.add(new LongDecrypter1(-4617416675770205927L));
    f.add(new LongDecrypter1(3256576131634881107L));
    f.add(new LongDecrypter1(8510773107491051585L));
    f.add(new LongDecrypter1(7365138421256309114L));
    f.add(new LongDecrypter1(163571525502003729L));
    f.add(new LongDecrypter1(-8160441735082537413L));
    f.add(new LongDecrypter1(-9127840326063847917L));
    f.add(new LongDecrypter1(8489083611002688562L));
    f.add(new LongDecrypter1(-6744934502626892772L));
    f.add(new LongDecrypter1(2221286548225247325L));
    f.add(new LongDecrypter1(-2962263413232090336L));
    f.add(new LongDecrypter1(3934146036784200195L));
    f.add(new LongDecrypter1(-3436771769251688178L));
    f.add(new LongDecrypter1(-571685545082449314L));
    f.add(new LongDecrypter1(-3945694407626549965L));
    f.add(new LongDecrypter1(8845643057329656427L));
    f.add(new LongDecrypter1(-114004742351245484L));
    f.add(new LongDecrypter1(-7563862587164857524L));
    f.add(new LongDecrypter1(-5389753993406410661L));
    f.add(new LongDecrypter1(-160132178394707695L));
    f.add(new LongDecrypter1(-6183523531436058364L));
    f.add(new LongDecrypter1(-214828784118509446L));
    f.add(new LongDecrypter1(-1772884462377548962L));
    f.add(new LongDecrypter1(8949315185481553926L));
    f.add(new LongDecrypter1(-3729103974117941717L));
    f.add(new LongDecrypter1(4068251328299929969L));
    f.add(new LongDecrypter1(-997635462382512347L));
    f.add(new LongDecrypter1(1317888929730875933L));
    f.add(new LongDecrypter1(-7127247260880184787L));
    f.add(new LongDecrypter1(-5537235031471907997L));
    f.add(new LongDecrypter1(-5877154569661868784L));
    f.add(new LongDecrypter1(915032465674082373L));
    f.add(new LongDecrypter1(-3696594469398488735L));
    f.add(new LongDecrypter1(1198701298961969789L));
    f.add(new LongDecrypter1(-8641144559498016152L));
    f.add(new LongDecrypter1(4064835476831462925L));
    f.add(new LongDecrypter1(-2841196238079137667L));
    f.add(new LongDecrypter1(-3099776064505963192L));
    f.add(new LongDecrypter1(2939614047086402172L));
    f.add(new LongDecrypter1(9071330913751975238L));
    f.add(new LongDecrypter1(5120371539735949397L));
    f.add(new LongDecrypter1(2962658612416286234L));
    f.add(new LongDecrypter1(6689004982062057288L));
    f.add(new LongDecrypter1(8964882758012483870L));
    f.add(new LongDecrypter1(5475469752228117537L));
    f.add(new LongDecrypter1(-2984707355842012321L));
    f.add(new LongDecrypter1(5597108390879478379L));
    f.add(new LongDecrypter1(394761154282362208L));
    f.add(new LongDecrypter1(1230930935480479505L));
    f.add(new LongDecrypter1(-8396531332138972334L));
    f.add(new LongDecrypter1(8950390791323304538L));
    f.add(new LongDecrypter1(7341335208455734582L));
    f.add(new LongDecrypter1(-2511358839460762368L));
    f.add(new LongDecrypter1(8749169917523618680L));
    f.add(new LongDecrypter1(-3629581727734455696L));
    f.add(new LongDecrypter1(-2494960099271702435L));
    f.add(new LongDecrypter1(1333200973290266984L));
    f.add(new LongDecrypter1(5220533190940070166L));
    f.add(new LongDecrypter1(7182310048075607823L));
    f.add(new LongDecrypter1(-4992762815153309925L));
    f.add(new LongDecrypter1(-1567701269007552940L));
    f.add(new LongDecrypter1(-6233143081381465527L));
    f.add(new LongDecrypter1(-5356460109429705702L));
    f.add(new LongDecrypter1(2921817814179584568L));
    f.add(new LongDecrypter1(5157239356123225381L));
    f.add(new LongDecrypter1(6545866594900197983L));
    f.add(new LongDecrypter1(1723419563101916175L));
    f.add(new LongDecrypter1(1768072117978956882L));
    f.add(new LongDecrypter1(-1697426504002076120L));
    f.add(new LongDecrypter1(-4693519658482978727L));
    f.add(new LongDecrypter1(-6477458318850806217L));
    f.add(new LongDecrypter1(-2578676782114461676L));
    f.add(new LongDecrypter1(-4822563471147350265L));
    f.add(new LongDecrypter1(-4644685633478581922L));
    f.add(new LongDecrypter1(-2077374908101105818L));
    f.add(new LongDecrypter1(7772646909634991452L));
    f.add(new LongDecrypter1(-4802873170356544924L));
    f.add(new LongDecrypter1(-4732105155733304966L));
    f.add(new LongDecrypter1(3191653724556036960L));
    f.add(new LongDecrypter1(-6675909975872865506L));
    f.add(new LongDecrypter1(-4370639611625329080L));
    f.add(new LongDecrypter1(6127069360154651222L));
    f.add(new LongDecrypter1(-3830571812126989260L));
    f.add(new LongDecrypter1(-1145135241143221176L));
    f.add(new LongDecrypter1(5757803355491053851L));
    f.add(new LongDecrypter1(5861514532574319479L));
    f.add(new LongDecrypter1(5029961575385880373L));
    f.add(new LongDecrypter1(-4737356065818941110L));
    f.add(new LongDecrypter1(-1997637500916576252L));
    f.add(new LongDecrypter1(-402317396686749838L));
    f.add(new LongDecrypter1(-5490296329405198209L));
    f.add(new LongDecrypter1(7015982033247445319L));
    f.add(new LongDecrypter1(5566428372603202648L));
    f.add(new LongDecrypter1(1610547181022171706L));
    f.add(new LongDecrypter1(-749958015179314652L));
    f.add(new LongDecrypter1(-3287041484289893793L));
    f.add(new LongDecrypter1(1205591141138602038L));
    f.add(new LongDecrypter1(3244013406560206393L));
    f.add(new LongDecrypter1(-4381897587511407242L));
    f.add(new LongDecrypter1(2769134562871130661L));
    f.add(new LongDecrypter1(-5510210567648891639L));
    f.add(new LongDecrypter1(-6315616873848322948L));
    f.add(new LongDecrypter1(3704471921972883308L));
    f.add(new LongDecrypter1(-628270213006929303L));
    f.add(new LongDecrypter1(2929554665206796590L));
    f.add(new LongDecrypter1(-5394171273559455145L));
    f.add(new LongDecrypter1(5396731502047557482L));
    f.add(new LongDecrypter1(-4573704703133599927L));
    f.add(new LongDecrypter1(380442719608947285L));
    f.add(new LongDecrypter1(4819409575663658013L));
    f.add(new LongDecrypter1(6093821547247321433L));
    f.add(new LongDecrypter1(1244195524525771376L));
    f.add(new LongDecrypter1(-1490733409722229745L));
    f.add(new LongDecrypter1(-7846179103121209336L));
    f.add(new LongDecrypter1(6793493318428248099L));
    f.add(new LongDecrypter1(1878180836146942289L));
    f.add(new LongDecrypter1(9127210875299794782L));
    f.add(new LongDecrypter1(-3121621012635395049L));
    f.add(new LongDecrypter1(5627575452836567811L));
    f.add(new LongDecrypter1(4345720246612391945L));
    f.add(new LongDecrypter1(-2987207132099937157L));
    f.add(new LongDecrypter1(2718525029608824611L));
    f.add(new LongDecrypter1(1928462778601061980L));
    f.add(new LongDecrypter1(8726174620560326418L));
    f.add(new LongDecrypter1(3742312471085449325L));
    f.add(new LongDecrypter1(9172196962600855678L));
    f.add(new LongDecrypter1(-8457539779541178549L));
    f.add(new LongDecrypter1(5148847283195559037L));
    f.add(new LongDecrypter1(-6287796511558536662L));
    f.add(new LongDecrypter1(6728069329333144688L));
    f.add(new LongDecrypter1(-728759495188423120L));
    mutableNumberPool = (int[]) mutableNumberPool.clone();
    f.add(new LongDecrypter1(3123981817461308697L));
    f.add(new LongDecrypter1(-9069789736011624704L));
    f.add(new LongDecrypter1(-323664384510299095L));
    f.add(new LongDecrypter1(7371305299622091599L));
    f.add(new LongDecrypter1(-5118654025279893444L));
    f.add(new LongDecrypter1(8914889255355890013L));
    f.add(new LongDecrypter1(-6341878152146381740L));
    f.add(new LongDecrypter1(8719301639867663453L));
    f.add(new LongDecrypter1(1447485275231485462L));
    f.add(new LongDecrypter1(3000218305613173085L));
    f.add(new LongDecrypter1(5628421437479278969L));
    f.add(new LongDecrypter1(-464022640854493676L));
    f.add(new LongDecrypter1(-4503733056757245403L));
    f.add(new LongDecrypter1(5119989142457130753L));
    f.add(new LongDecrypter1(9057318678582108161L));
    f.add(new LongDecrypter1(1490790573133747500L));
    f.add(new LongDecrypter1(-6526284619751243769L));
    f.add(new LongDecrypter1(7948142709974031419L));
    f.add(new LongDecrypter1(-166781528820860339L));
    f.add(new LongDecrypter1(-2097328693574984093L));
    f.add(new LongDecrypter1(-4846623685971182474L));
    f.add(new LongDecrypter1(7404752823167592293L));
    f.add(new LongDecrypter1(-4655249128411810447L));
    f.add(new LongDecrypter1(3324069469948004682L));
    f.add(new LongDecrypter1(-3682584884556362398L));
    f.add(new LongDecrypter1(-2634301777958225283L));
    f.add(new LongDecrypter1(2019191113204726620L));
    f.add(new LongDecrypter1(-9095504018929749741L));
    f.add(new LongDecrypter1(-7768597088751846295L));
    f.add(new LongDecrypter1(7827637957870423658L));
    f.add(new LongDecrypter1(2590720253343633763L));
    f.add(new LongDecrypter1(-2074445615499076596L));
    f.add(new LongDecrypter1(4478877478060189823L));
    f.add(new LongDecrypter1(3871648429399759137L));
    f.add(new LongDecrypter1(5780224641161271655L));
    f.add(new LongDecrypter1(-587307018214525586L));
    f.add(new LongDecrypter1(-4713977051001056438L));
    f.add(new LongDecrypter1(8591023449897235973L));
    f.add(new LongDecrypter1(6496683859634571100L));
    f.add(new LongDecrypter1(-472878060695501236L));
    f.add(new LongDecrypter1(-3840600464305262995L));
    f.add(new LongDecrypter1(-3589405155502518259L));
    f.add(new LongDecrypter1(2763248056044434997L));
    f.add(new LongDecrypter1(4692043840546359380L));
    f.add(new LongDecrypter1(-1848123464957624244L));
    f.add(new LongDecrypter1(2045980452423856442L));
    f.add(new LongDecrypter1(-117892202448721551L));
    f.add(new LongDecrypter1(-5697288768445779184L));
    f.add(new LongDecrypter1(-2276005181197399426L));
    f.add(new LongDecrypter1(9174021919578190095L));
    f.add(new LongDecrypter1(8414178418449687143L));
    f.add(new LongDecrypter1(964909248668401510L));
    f.add(new LongDecrypter1(-6420089835907853503L));
    f.add(new LongDecrypter1(1206668257124389910L));
    f.add(new LongDecrypter1(5228800532249141616L));
    f.add(new LongDecrypter1(4369205978830936126L));
    f.add(new LongDecrypter1(7729861559149491830L));
    f.add(new LongDecrypter1(-4683709738109657062L));
    f.add(new LongDecrypter1(-8134414797632320240L));
    f.add(new LongDecrypter1(1710424857346228286L));
    f.add(new LongDecrypter1(4533787663450792213L));
    f.add(new LongDecrypter1(5250712491123428927L));
    f.add(new LongDecrypter1(-7399998953575112158L));
    f.add(new LongDecrypter1(-7503459437579941063L));
    f.add(new LongDecrypter1(1759656563388384806L));
    f.add(new LongDecrypter1(6049160167533009731L));
    f.add(new LongDecrypter1(5662735568805587576L));
    f.add(new LongDecrypter1(-3287329673264815835L));
    f.add(new LongDecrypter1(-7202674938420357883L));
    f.add(new LongDecrypter1(8264077325419788549L));
    f.add(new LongDecrypter1(105829686699111191L));
    f.add(new LongDecrypter1(8639859108793276933L));
    f.add(new LongDecrypter1(-7929899358064994728L));
    f.add(new LongDecrypter1(-5603830156674993046L));
    f.add(new LongDecrypter1(3065385947166298415L));
    f.add(new LongDecrypter1(1590803408820128797L));
    f.add(new LongDecrypter1(8068534516492922392L));
    f.add(new LongDecrypter1(-8006462752586302146L));
    f.add(new LongDecrypter1(4415286755741161732L));
    f.add(new LongDecrypter1(7579467872633793618L));
    mutableNumberPool = (int[]) mutableNumberPool.clone();
    f.add(new LongDecrypter1(3353689417665180214L));
    f.add(new LongDecrypter1(-4890728051061267630L));
    f.add(new LongDecrypter1(-7765364972116597192L));
    f.add(new LongDecrypter1(-2950975812271981995L));
    f.add(new LongDecrypter1(-1865222533820919226L));
    f.add(new LongDecrypter1(-5935273144739166183L));
    f.add(new LongDecrypter1(-8726441470434619326L));
    f.add(new LongDecrypter1(6537978008560483175L));
    f.add(new LongDecrypter1(-4729355686354735841L));
    f.add(new LongDecrypter1(5261917703163211105L));
    f.add(new LongDecrypter1(382694519422632533L));
    f.add(new LongDecrypter1(-6230649450814295490L));
    f.add(new LongDecrypter1(-7011501018073908618L));
    f.add(new LongDecrypter1(8480807668178964840L));
    f.add(new LongDecrypter1(7316813999135947873L));
    f.add(new LongDecrypter1(-1978630551954399630L));
    f.add(new LongDecrypter1(-3450396147663272072L));
    f.add(new LongDecrypter1(8566344773080147277L));
    f.add(new LongDecrypter1(-7130493993344824022L));
    f.add(new LongDecrypter1(3074040277587997737L));
    f.add(new LongDecrypter1(-3215216590703372718L));
    f.add(new LongDecrypter1(668392685647621974L));
    f.add(new LongDecrypter1(-8299355380725830290L));
    f.add(new LongDecrypter1(-2141160178662731760L));
    f.add(new LongDecrypter1(-5552714407979987888L));
    f.add(new LongDecrypter1(-8514174249124482292L));
    f.add(new LongDecrypter1(-267538969170167187L));
    f.add(new LongDecrypter1(-6015945704533227926L));
    f.add(new LongDecrypter1(2848645146241511730L));
    f.add(new LongDecrypter1(8732403067646632451L));
    f.add(new LongDecrypter1(7447266920259331606L));
    f.add(new LongDecrypter1(3746081854878348656L));
    f.add(new LongDecrypter1(9027048670226612759L));
    f.add(new LongDecrypter1(-5150537824674051723L));
    f.add(new LongDecrypter1(-8856255601204710879L));
    f.add(new LongDecrypter1(-1603063408204147182L));
    f.add(new LongDecrypter1(-4379963049119602342L));
    f.add(new LongDecrypter1(-2354106643511466664L));
    f.add(new LongDecrypter1(-8438222519059515827L));
    f.add(new LongDecrypter1(-8525241284021267915L));
    f.add(new LongDecrypter1(-4713631510949118869L));
    f.add(new LongDecrypter1(5494197952805683322L));
    f.add(new LongDecrypter1(-2803714062836393466L));
    f.add(new LongDecrypter1(834350807942076220L));
    f.add(new LongDecrypter1(-2803025750978137237L));
    f.add(new LongDecrypter1(-3639592030903621369L));
    f.add(new LongDecrypter1(-3205221769422207620L));
    f.add(new LongDecrypter1(6137169999648429155L));
    f.add(new LongDecrypter1(1845155904071928601L));
    f.add(new LongDecrypter1(1881462987446087967L));
    f.add(new LongDecrypter1(8994150716039412784L));
    f.add(new LongDecrypter1(-689266450110917261L));
    f.add(new LongDecrypter1(2870767467022586685L));
    f.add(new LongDecrypter1(3416893677099904837L));
    f.add(new LongDecrypter1(5604288906048632072L));
    f.add(new LongDecrypter1(4794574540075488122L));
    f.add(new LongDecrypter1(-8873369584428817349L));
    f.add(new LongDecrypter1(-4199752890653780871L));
    f.add(new LongDecrypter1(-1746150380386033081L));
    f.add(new LongDecrypter1(-7275157191837229502L));
    f.add(new LongDecrypter1(1464668239948125706L));
    f.add(new LongDecrypter1(-1989670409584258754L));
    f.add(new LongDecrypter1(-4100623189401528040L));
    f.add(new LongDecrypter1(-9022342751689801679L));
    f.add(new LongDecrypter1(-1297874714300681867L));
    f.add(new LongDecrypter1(-6984393055181507250L));
    f.add(new LongDecrypter1(-859300423905213946L));
    f.add(new LongDecrypter1(-5175754230219423683L));
    f.add(new LongDecrypter1(4679689865775202595L));
    f.add(new LongDecrypter1(2848532333699623434L));
    f.add(new LongDecrypter1(-6101318258513184912L));
    f.add(new LongDecrypter1(-4916973882150029207L));
    f.add(new LongDecrypter1(-6277451035331749555L));
    f.add(new LongDecrypter1(980234207379701280L));
    f.add(new LongDecrypter1(6620208133880536149L));
    f.add(new LongDecrypter1(3363289285053783387L));
    f.add(new LongDecrypter1(-3175150297517903003L));
    f.add(new LongDecrypter1(-8600416641602506940L));
    f.add(new LongDecrypter1(-3299980457845877485L));
    f.add(new LongDecrypter1(2123386905965069918L));
    f.add(new LongDecrypter1(6114029935015278708L));
    f.add(new LongDecrypter1(-3371568838790535065L));
    f.add(new LongDecrypter1(-5278880873848158683L));
    f.add(new LongDecrypter1(3620838319557240908L));
    f.add(new LongDecrypter1(-8316126234860352408L));
    f.add(new LongDecrypter1(2128711266520349557L));
    f.add(new LongDecrypter1(1660862800821763670L));
    f.add(new LongDecrypter1(-8027156411800809878L));
    f.add(new LongDecrypter1(1090949195215716356L));
    f.add(new LongDecrypter1(3849647630193561094L));
    f.add(new LongDecrypter1(-5254885491080214472L));
    f.add(new LongDecrypter1(6204725911977086024L));
    f.add(new LongDecrypter1(6707797091292015721L));
    f.add(new LongDecrypter1(-1988124221269519528L));
    f.add(new LongDecrypter1(5092529663763261197L));
    f.add(new LongDecrypter1(2016028107744740408L));
    f.add(new LongDecrypter1(2900481677748932119L));
    f.add(new LongDecrypter1(648173356358948150L));
    f.add(new LongDecrypter1(-4226564601689169858L));
    f.add(new LongDecrypter1(-4335322383944857512L));
    f.add(new LongDecrypter1(-6016061986573194912L));
    f.add(new LongDecrypter1(-7176060433647764159L));
    f.add(new LongDecrypter1(2942344384811892567L));
    f.add(new LongDecrypter1(6092988999849727017L));
    f.add(new LongDecrypter1(3945225378372948229L));
    f.add(new LongDecrypter1(-5645814257619978902L));
    f.add(new LongDecrypter1(1785667623295804164L));
    f.add(new LongDecrypter1(5101505602929939221L));
    f.add(new LongDecrypter1(5487859664203196249L));
    f.add(new LongDecrypter1(-5720851097316669648L));
    f.add(new LongDecrypter1(-3785204615371068055L));
    f.add(new LongDecrypter1(-561986025123427258L));
    f.add(new LongDecrypter1(470761896148187473L));
    f.add(new LongDecrypter1(-857798012934833099L));
    f.add(new LongDecrypter1(-6330894209310279593L));
    f.add(new LongDecrypter1(7139268433438188557L));
    f.add(new LongDecrypter1(9106837350205382004L));
    f.add(new LongDecrypter1(5411332552963557386L));
    f.add(new LongDecrypter1(7933151961286681106L));
    f.add(new LongDecrypter1(-2892079223345912974L));
    f.add(new LongDecrypter1(6982561687063692631L));
    f.add(new LongDecrypter1(2719761414824180064L));
    f.add(new LongDecrypter1(-8728583861384937694L));
    f.add(new LongDecrypter1(-2321847920782466177L));
    f.add(new LongDecrypter1(2777521335515563352L));
    f.add(new LongDecrypter1(-5004973603669270194L));
    f.add(new LongDecrypter1(9163881514320632424L));
    f.add(new LongDecrypter1(-4450156830168333756L));
    f.add(new LongDecrypter1(9022710403794182422L));
    f.add(new LongDecrypter1(7843972757221422894L));
    f.add(new LongDecrypter1(-8447990974203036899L));
    f.add(new LongDecrypter1(5878388923139080493L));
    f.add(new LongDecrypter1(-4133067880340619408L));
    f.add(new LongDecrypter1(4343648983921339206L));
    f.add(new LongDecrypter1(5024685564347393621L));
    f.add(new LongDecrypter1(4866371535087567714L));
    f.add(new LongDecrypter1(2628180531315318072L));
    f.add(new LongDecrypter1(-8972810740805988305L));
    f.add(new LongDecrypter1(-8010469905465125831L));
    f.add(new LongDecrypter1(-7154064171073873332L));
    f.add(new LongDecrypter1(1461356700224937042L));
    f.add(new LongDecrypter1(98801013476167000L));
    f.add(new LongDecrypter1(-630565617635933577L));
    f.add(new LongDecrypter1(-2923660292679716530L));
    f.add(new LongDecrypter1(-8000052327432781205L));
    f.add(new LongDecrypter1(2877340485581057856L));
    f.add(new LongDecrypter1(-2700898613642777271L));
    f.add(new LongDecrypter1(5199370076986222699L));
    f.add(new LongDecrypter1(-7581939487537450731L));
    f.add(new LongDecrypter1(2179653435652298003L));
    f.add(new LongDecrypter1(-1345552540003111049L));
    f.add(new LongDecrypter1(1027617938134274568L));
    f.add(new LongDecrypter1(-8315145766711593684L));
    f.add(new LongDecrypter1(2780905536902388534L));
    f.add(new LongDecrypter1(-1667862483904418024L));
    f.add(new LongDecrypter1(-3583275351917121211L));
    f.add(new LongDecrypter1(-4737090727306786044L));
    f.add(new LongDecrypter1(1331216929299961183L));
    f.add(new LongDecrypter1(-1241739169435989729L));
    f.add(new LongDecrypter1(-1021050309461968010L));
    f.add(new LongDecrypter1(8949170418377121573L));
    f.add(new LongDecrypter1(-1027276572384110223L));
    f.add(new LongDecrypter1(-550100803298838720L));
    f.add(new LongDecrypter1(-920235981772234156L));
    f.add(new LongDecrypter1(1998750113306284382L));
    f.add(new LongDecrypter1(-9143248736720514975L));
    f.add(new LongDecrypter1(-7818190187401558403L));
    f.add(new LongDecrypter1(-3162165788155607192L));
    f.add(new LongDecrypter1(5414940549013959692L));
    f.add(new LongDecrypter1(4806474729330816058L));
    f.add(new LongDecrypter1(5314988679666910211L));
    f.add(new LongDecrypter1(6982668010006073115L));
    f.add(new LongDecrypter1(-2465590116038820228L));
    f.add(new LongDecrypter1(6526893981964949074L));
    f.add(new LongDecrypter1(-6194151172155161281L));
    f.add(new LongDecrypter1(8630599462842298898L));
    f.add(new LongDecrypter1(-22056709181439950L));
    f.add(new LongDecrypter1(7841026110869319964L));
    f.add(new LongDecrypter1(-3431964442040488336L));
    f.add(new LongDecrypter1(4336759233672801839L));
    f.add(new LongDecrypter1(-4818018289031333811L));
    f.add(new LongDecrypter1(1858801153554425710L));
    f.add(new LongDecrypter1(-5407591726371152001L));
    f.add(new LongDecrypter1(458869154562108713L));
    f.add(new LongDecrypter1(-6120672096660523923L));
    f.add(new LongDecrypter1(-1573077873219249141L));
    f.add(new LongDecrypter1(-3400235870762790794L));
    f.add(new LongDecrypter1(8603852188278464302L));
    f.add(new LongDecrypter1(-216471682221518735L));
    f.add(new LongDecrypter1(4958488947543730786L));
    f.add(new LongDecrypter1(2617601700128523360L));
    f.add(new LongDecrypter1(1935099800016483434L));
    f.add(new LongDecrypter1(-8851653639451196121L));
    f.add(new LongDecrypter1(703647420220481895L));
    f.add(new LongDecrypter1(-5829641327573384108L));
    f.add(new LongDecrypter1(6701836975809850721L));
    f.add(new LongDecrypter1(-1728184293609821078L));
    f.add(new LongDecrypter1(-1802124911314707856L));
    f.add(new LongDecrypter1(2649808558380673853L));
    f.add(new LongDecrypter1(-8770921637106683093L));
    f.add(new LongDecrypter1(-9022305052714319615L));
    f.add(new LongDecrypter1(-3641495095642281187L));
    f.add(new LongDecrypter1(-4729485338924484294L));
    f.add(new LongDecrypter1(5163113304774476137L));
    f.add(new LongDecrypter1(6121189468112493070L));
    f.add(new LongDecrypter1(2260753270295613747L));
    f.add(new LongDecrypter1(5447368382983191900L));
    f.add(new LongDecrypter1(-2783852683762409981L));
    f.add(new LongDecrypter1(2982911575606719503L));
    f.add(new LongDecrypter1(418506796835376732L));
    f.add(new LongDecrypter1(4573654158771594273L));
    f.add(new LongDecrypter1(6089619787186807407L));
    f.add(new LongDecrypter1(-8024073409900724384L));
    f.add(new LongDecrypter1(9133873434019287659L));
    f.add(new LongDecrypter1(1348424155780968308L));
    f.add(new LongDecrypter1(6513276877409274716L));
    f.add(new LongDecrypter1(-7817902869606204298L));
    f.add(new LongDecrypter1(-7067165855987920604L));
    f.add(new LongDecrypter1(2002520142872203095L));
    f.add(new LongDecrypter1(-6087898490378273711L));
    f.add(new LongDecrypter1(8743249348057432853L));
    f.add(new LongDecrypter1(5155296734268770562L));
    f.add(new LongDecrypter1(-2383373866280366833L));
    f.add(new LongDecrypter1(5044812922513473382L));
    f.add(new LongDecrypter1(-3078837164674525831L));
    f.add(new LongDecrypter1(-4062729010651707550L));
    f.add(new LongDecrypter1(7857124971844231711L));
    f.add(new LongDecrypter1(5457968541179511401L));
    f.add(new LongDecrypter1(-5756814311169447862L));
    f.add(new LongDecrypter1(-2328398497679838418L));
    f.add(new LongDecrypter1(5037984042970560812L));
    f.add(new LongDecrypter1(-5821246296106742657L));
    f.add(new LongDecrypter1(-5598522630953587699L));
    f.add(new LongDecrypter1(-6915720826048005328L));
    f.add(new LongDecrypter1(-1757531524893795772L));
    f.add(new LongDecrypter1(7711366347596515359L));
    f.add(new LongDecrypter1(-2508344940770375843L));
    f.add(new LongDecrypter1(-1958901065989171903L));
    f.add(new LongDecrypter1(6593865677046990909L));
    mutableNumberPool = (int[]) mutableNumberPool.clone();
    f.add(new LongDecrypter1(4294148454648727092L));
    f.add(new LongDecrypter1(-1403135501874259611L));
    f.add(new LongDecrypter1(669894455170059330L));
    f.add(new LongDecrypter1(5969115259296313885L));
    f.add(new LongDecrypter1(6006074756119032913L));
    f.add(new LongDecrypter1(-7087094550249005235L));
    f.add(new LongDecrypter1(-1700128685116273294L));
    f.add(new LongDecrypter1(-2701714164910619092L));
    f.add(new LongDecrypter1(3750132386748992080L));
    f.add(new LongDecrypter1(8055117132173116784L));
    f.add(new LongDecrypter1(-8022141185680785037L));
    f.add(new LongDecrypter1(6003798324848002385L));
    f.add(new LongDecrypter1(7864245387436781866L));
    f.add(new LongDecrypter1(-2432284427432697735L));
    f.add(new LongDecrypter1(-5723260322880248553L));
    f.add(new LongDecrypter1(-8231066441305293495L));
    f.add(new LongDecrypter1(-5716782213702986430L));
    f.add(new LongDecrypter1(-6065315107958657453L));
    f.add(new LongDecrypter1(-8089463602689788585L));
    f.add(new LongDecrypter1(8267949031799818814L));
    f.add(new LongDecrypter1(-501014590016411067L));
    f.add(new LongDecrypter1(-7417813050321278416L));
    f.add(new LongDecrypter1(8467561015506104600L));
    f.add(new LongDecrypter1(-953182606337653900L));
    f.add(new LongDecrypter1(-4409572277925425069L));
    f.add(new LongDecrypter1(-2984150602136508061L));
    f.add(new LongDecrypter1(2425815659940105290L));
    f.add(new LongDecrypter1(6202629222000764253L));
    f.add(new LongDecrypter1(3167090477094296677L));
    f.add(new LongDecrypter1(2375016374088837392L));
    f.add(new LongDecrypter1(1755159740002316903L));
    f.add(new LongDecrypter1(5816256118218694922L));
    f.add(new LongDecrypter1(-8371596391687840489L));
    f.add(new LongDecrypter1(-8946268129329818829L));
    f.add(new LongDecrypter1(7691106236346789404L));
    f.add(new LongDecrypter1(5347372426458345493L));
    f.add(new LongDecrypter1(-795585369112892359L));
    f.add(new LongDecrypter1(8589521086970508554L));
    f.add(new LongDecrypter1(-5491431801700760458L));
    f.add(new LongDecrypter1(-3653459661474739192L));
    f.add(new LongDecrypter1(1965948575058990346L));
    f.add(new LongDecrypter1(4108480581253664294L));
    f.add(new LongDecrypter1(2525337852744631316L));
    f.add(new LongDecrypter1(-3976816553450396890L));
    f.add(new LongDecrypter1(8903072313838479922L));
    f.add(new LongDecrypter1(2292313479567609205L));
    f.add(new LongDecrypter1(4722844739823544416L));
    f.add(new LongDecrypter1(7686699201968476699L));
    f.add(new LongDecrypter1(-6562554514220268235L));
    f.add(new LongDecrypter1(2320794911495513669L));
    f.add(new LongDecrypter1(7320299521338147397L));
    f.add(new LongDecrypter1(2805911437997031956L));
    f.add(new LongDecrypter1(683020884408656649L));
    f.add(new LongDecrypter1(6126207642105232432L));
    f.add(new LongDecrypter1(-7176016269284514297L));
    f.add(new LongDecrypter1(-8033835911069254896L));
    f.add(new LongDecrypter1(-4521680277892617610L));
    f.add(new LongDecrypter1(-7722464984245934535L));
    f.add(new LongDecrypter1(-3864758102832358841L));
    f.add(new LongDecrypter1(3136819754763617106L));
    f.add(new LongDecrypter1(4612127509001437001L));
    f.add(new LongDecrypter1(4877576988565574146L));
    f.add(new LongDecrypter1(-3993831536360412311L));
    f.add(new LongDecrypter1(3497127873665015327L));
    f.add(new LongDecrypter1(810648566345487735L));
    f.add(new LongDecrypter1(212666162098105862L));
    f.add(new LongDecrypter1(5248894219456725363L));
    f.add(new LongDecrypter1(3300269710224254300L));
    f.add(new LongDecrypter1(8846503112951411755L));
    f.add(new LongDecrypter1(6109567338086971938L));
    f.add(new LongDecrypter1(5251722195395330154L));
    f.add(new LongDecrypter1(-5330291147265649367L));
    f.add(new LongDecrypter1(7565058400581352239L));
    f.add(new LongDecrypter1(-4211887087490862059L));
    f.add(new LongDecrypter1(-4939719026944369907L));
    f.add(new LongDecrypter1(-4983052400759416743L));
    f.add(new LongDecrypter1(3542674163069123398L));
    f.add(new LongDecrypter1(-1487119450460742355L));
    f.add(new LongDecrypter1(3910139209092479572L));
    f.add(new LongDecrypter1(5393760485569384472L));
    f.add(new LongDecrypter1(-7428051292777401773L));
    f.add(new LongDecrypter1(-5438727553116681666L));
    f.add(new LongDecrypter1(-2649220214809800625L));
    f.add(new LongDecrypter1(8597908996122952484L));
    f.add(new LongDecrypter1(2101404646864295476L));
    mutableNumberPool = new int[]{
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
