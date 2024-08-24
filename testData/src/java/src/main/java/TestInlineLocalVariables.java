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
}
