package zelix.longdecrypter;

public interface ILongDecrypter {
  void setChild(ILongDecrypter parent);

  int[] getEncryptionInts();

  long decrypt(long decryptKey);

  void setKey(long key);

  boolean lessThanOrEqual(ILongDecrypter other);
}
