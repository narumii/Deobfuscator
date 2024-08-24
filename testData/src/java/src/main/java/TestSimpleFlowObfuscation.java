public class TestSimpleFlowObfuscation {
  public void testFlow() {
    int a = 1;
    if (a == 1) {
      a = 2;
      if (a == 2) {
        a = 3;
        if (System.currentTimeMillis() == 123) {
          a = 4;
          if (a == 4) {
            a = 5;
            if (a == 5) {
              a = 6;
              if (a == 6) {
                System.out.println("123");
              } else {
                throw new RuntimeException();
              }
            } else {
              throw new RuntimeException();
            }
          } else {
            throw new RuntimeException();
          }
        }
      } else {
        throw new RuntimeException();
      }
    } else {
      throw new RuntimeException();
    }
  }

  public void compareTest() {
    int a = 123;

    if (a == 100) {
      System.out.println("a is 100");
    } else {
      System.out.println("a is not 100");
    }

    // TODO: Simplify also this
    while (a * 321 == 100) {
      a += 1;
    }
  }
}
