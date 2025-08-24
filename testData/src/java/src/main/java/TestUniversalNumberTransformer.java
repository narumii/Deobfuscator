public class TestUniversalNumberTransformer {
  public void testNumbers1() {
    int a = 1 + 2;
    int b = 235434535 / 12323432;
    double c = 123123.123123 * 123123.123123;
    float d = 123123.123123f / 12.34f;

    System.out.println(a + b + c + d);

    int constant = 69420;
    for (int i = 0; i < 10; i++) {
      System.out.println(i + constant);
    }
  }

  public void charMath() {
    int a = 49152;
    int b = 14828;
    int c = (char) a >>> b;
    System.out.println(c);
  }

  public void illegalOperations() {
    int a = 2;
    if (a == 0) {
      // Transformer shouldn't touch it
      int b = 9 / 0;
      System.out.println(b);
      System.out.println(Integer.parseInt("aaa"));
      System.out.println(Float.parseFloat("bbb"));
      System.out.println(Double.parseDouble("ccc"));
    }
  }

  public void numberCasts() {
    long temp1 = 3458347593845798359L;
    int a = (int) temp1;
    long temp2 = 6873457893573587384L;
    float b = (float) temp2;
    double temp3 = 23423.3241212121212125433534534523423464523423454567568354345354354354354345345354534534534345345534389467459867498679845687459867984576894589679845689458969847545343543544d;
    float c = (float) temp3;
  }

  public void methodCallsOnLiterals() {
    int a = "dfgdfgdfgdfg".length();
    int b = "asddsf".hashCode();
    int c = Integer.parseInt("123");
    int d = Integer.parseInt("1010", 2);
    int e = Integer.reverse(3456);
    long f = Long.reverse(1234L);

    int g = Float.floatToIntBits(123.123f);
    float h = Float.intBitsToFloat(123);

    long i = Double.doubleToLongBits(123.123123123d);
    double j = Double.longBitsToDouble(123123L);
  }
}
