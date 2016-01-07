package Utils

import java.security._
import java.security.spec.X509EncodedKeySpec
import javax.crypto.{SecretKey, KeyGenerator, Cipher}
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}

import Objects.{BaseObject, SecureMessage, SecureObject}

object Crypto {
  def generateRSAKeys(): KeyPair = {
    val keyGen = KeyPairGenerator.getInstance("RSA")
    val random = SecureRandom.getInstance("SHA1PRNG")
    keyGen.initialize(1024, random)
    keyGen.generateKeyPair()
  }

  def generateAESKey(): SecretKey = {
    val keyGen = KeyGenerator.getInstance("AES")
    keyGen.init(128)
    keyGen.generateKey()
  }

  def encryptRSA(bytes: Array[Byte], publicKey: Key): Array[Byte] = {
    val encipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    encipher.init(Cipher.ENCRYPT_MODE, publicKey)
    encipher.doFinal(bytes)
  }

  def decryptRSA(bytes: Array[Byte], privateKey: Key): Array[Byte] = {
    val encipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    encipher.init(Cipher.DECRYPT_MODE, privateKey)
    encipher.doFinal(bytes)
  }

  def encryptAES(bytes: Array[Byte], secret: Key, iv: Array[Byte]): Array[Byte] = {
    val encipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
    val skeySpec = new SecretKeySpec(secret.getEncoded(), "AES")
    val ivSpec = new IvParameterSpec(iv)
    encipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec)
    encipher.doFinal(bytes)
  }

  def decryptAES(bytes: Array[Byte], secret: Key, iv: Array[Byte]): Array[Byte] = {
    val encipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
    val skeySpec = new SecretKeySpec(secret.getEncoded(), "AES")
    val ivSpec = new IvParameterSpec(iv)
    encipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec)
    encipher.doFinal(bytes)
  }

  def byteArrayToHexString(bytes: Array[Byte]): String = {
    val keyString = new StringBuffer()
    for (i <- bytes) {
      keyString.append(Integer.toHexString(0x0100 + (i & 0x00FF)).substring(1))
    }
    keyString.toString()
  }

  def signData(privateKey: PrivateKey, data: Array[Byte]): Array[Byte] = {
    val sig = Signature.getInstance("SHA256withRSA")
    sig.initSign(privateKey)
    sig.update(sha512(data))
    sig.sign()
  }

  def verifySign(publicKey: PublicKey, signedData: Array[Byte], data: Array[Byte]): Boolean = {
    val sig = Signature.getInstance("SHA256withRSA")
    sig.initVerify(publicKey)
    sig.update(sha512(data))
    sig.verify(signedData)
  }

  def constructAESKeyFromBytes(bytes: Array[Byte]): SecretKey = {
    new SecretKeySpec(bytes, 0, bytes.length, "AES")
  }

  def constructRSAKeyFromBytes(bytes: Array[Byte]): Key = {
    new SecretKeySpec(bytes, 0, bytes.length, "RSA")
  }

  def constructRSAPublicKeyFromBytes(bytes: Array[Byte]): PublicKey = {
    KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes))
  }

  def constructRSAPrivateKeyFromBytes(bytes: Array[Byte]): PrivateKey = {
    KeyFactory.getInstance("RSA").generatePrivate(new X509EncodedKeySpec(bytes))
  }

  def md5(bytes: Array[Byte]): Array[Byte] = {
    MessageDigest.getInstance("MD5").digest(bytes)
  }

  def sha256(bytes: Array[Byte]): Array[Byte] = {
    MessageDigest.getInstance("SHA-256").digest(bytes)
  }

  def sha512(bytes: Array[Byte]): Array[Byte] = {
    MessageDigest.getInstance("SHA-512").digest(bytes)
  }

  def constructSecureObject(
                             baseObj: BaseObject,
                             from: Int,
                             to: Int,
                             objType: Int,
                             json: String,
                             publicKeys: Map[String, PublicKey]
                           ) = {
    val aesKey = generateAESKey()
    val encryptedKeys = publicKeys.map { case (pid, pubKey) =>
      (pid.toString, Crypto.encryptRSA(aesKey.getEncoded, pubKey))
    }

    SecureObject(
      baseObj,
      from,
      to,
      objType,
      Crypto.encryptAES(Base64Util.encodeBinary(json), aesKey, Constants.IV),
      encryptedKeys
    )
  }

  def constructSecureMessage(
                              pid: Int,
                              json: String,
                              theirPublicKey: PublicKey,
                              myPrivateKey: PrivateKey
                            ) = {
    val aesKey = generateAESKey()
    SecureMessage(
      pid,
      Crypto.encryptAES(Base64Util.encodeBinary(json), aesKey, Constants.IV),
      Crypto.signData(myPrivateKey, aesKey.getEncoded),
      Crypto.encryptRSA(aesKey.getEncoded, theirPublicKey)
    )
  }

}
