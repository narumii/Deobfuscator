package reverseengineering.zelix.longdecrypter;

public interface ILongDecrypter {
  void setParent(ILongDecrypter parent);

  int[] getEncryptionInts();

  long decrypt(long decryptKey);

  void setKey(long key);

  boolean equals(ILongDecrypter other);
}
