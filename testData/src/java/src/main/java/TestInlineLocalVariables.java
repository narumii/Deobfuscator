public class TestInlineLocalVariables {
  public static void test(int a) {
    System.out.println(a);
    if (System.currentTimeMillis() == 123) {
      a = 3;
    }
    // Should not inline
    System.out.println(a);
  }

  public static void test2() {
    int i = 1;
    System.out.println(i);
    i = 2;
    System.out.println(i);
    i = 3;
    System.out.println(i);
    i = 4;
    System.out.println(i);
  }

  public static boolean test3() {
    boolean testVar;
    try {
      testVar = System.currentTimeMillis() == 123;
    } finally {
      System.out.println("123");
    }

    return testVar;
  }

  public static void test4() {
    int a = 5;
    boolean b = true;
    long c = 123123123123123123L;
    double d = 123123.123123;
    float e = 123123.123123f;
    String f = "test";
    // Should not inline this
    Object someObj = new Object();

    System.out.println("" + a + b + c);
    System.out.println(d + e + f + someObj);
  }

  public static void mangle() {
    int a = 321;
    boolean b = true;
    long c = 657657;
    double d = 1455.45;
    float e = 89.345f;
    String f = "asd";
    Object someObj = new Object();

    System.out.println("" + a + b + c);
    System.out.println(d + e + f + someObj);
    try {
      a = 453453453;
      b = false;
      c = 7856783457834L;
      d = 34534535;
      try {
        e = 45354;
        f = "gfj";
        someObj = new Object();

        System.out.println("" + a + b + c);
      } catch (Exception ex) {
        a = 5;
        b = true;
        c = 465156165156L;
      }
      System.out.println(d + e + f + someObj);
    } catch (Exception ex) {
      a = 56876758;
      b = true;
      c = 5498489489L;
      d = 65654356.234d;
      e = 414541451.5455f;
      f = "hjk";
      someObj = new Object();

      System.out.println("" + a + b + c);
      System.out.println(d + e + f + someObj);
    }
  }
}
