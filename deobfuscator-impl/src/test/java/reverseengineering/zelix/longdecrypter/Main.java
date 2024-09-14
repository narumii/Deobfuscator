package reverseengineering.zelix.longdecrypter;

import java.lang.invoke.MethodHandles;

public class Main {
  // Usage

  public static void main(String[] args) {
    // LongDecrypter2 instances
    ILongDecrypter a1Instance = SimpleLongDecrypter.buildNumberDecryptor(900058104405336414L, 6106449219005125011L, MethodHandles.lookup().lookupClass());
    ILongDecrypter a2Instance = SimpleLongDecrypter.buildNumberDecryptor(5832394289974403481L, -8943439614781261032L, MethodHandles.lookup().lookupClass());
    //ILongDecrypter a2Instance = LongDecrypter1.buildNumberDecryptor(5832394123974403481L, -8943439614781261032L, MethodHandles.lookup().lookupClass());
    ILongDecrypter a3Instance = SimpleLongDecrypter.buildNumberDecryptor(-1563944528177415659L, 8240211990857304620L, MethodHandles.lookup().lookupClass());

    System.out.println(a1Instance);
    System.out.println(a2Instance);
    System.out.println(a3Instance);

    long a1 = a1Instance.decrypt(99062861074978L);
    long a2 = a2Instance.decrypt(19597665297729L);
    long a3 = a3Instance.decrypt(224919788586450L);

    System.out.println(a1); // 110160429747013
    System.out.println(a2); // 119662894797887
    System.out.println(a3); // 62547565276859

    // LongDecrypter2 instances
    ILongDecrypter b1Instance = SimpleLongDecrypter.buildNumberDecryptor(-2891110000934166428L, 4534565905501632758L, MethodHandles.lookup().lookupClass());
    ILongDecrypter b2Instance = SimpleLongDecrypter.buildNumberDecryptor(3109756102241299096L, 3300563161622516573L, MethodHandles.lookup().lookupClass());
    System.out.println(b1Instance);
    System.out.println(b2Instance);
    long b1 = b1Instance.decrypt(77751647876842L);
    long b2 = b2Instance.decrypt(3703773754795L);
    System.out.println(b1); // 97375689351077
    System.out.println(b2); // 92083991818967
  }
}
