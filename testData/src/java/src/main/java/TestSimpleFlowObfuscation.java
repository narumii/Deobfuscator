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
}
