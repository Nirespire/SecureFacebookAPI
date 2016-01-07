import Objects.ObjectTypes.ObjectType
import Objects.{Album, BaseObject}
import Objects.ObjectJsonSupport._
import Utils.{Base64Util, Constants, Crypto}
import org.scalatest.{FlatSpec, Matchers}

import spray.json._

import scala.util.Random

class CryptoSpec extends FlatSpec with Matchers {
  "The public key pair generator" should "generate 2 keys" in {

    val pair = Crypto.generateRSAKeys()

    val privateKey = pair.getPrivate().getEncoded()
    val publicKey = pair.getPublic().getEncoded()

    val md5Private = Crypto.md5(privateKey)
    val md5Public = Crypto.md5(publicKey)

    val sha256Private = Crypto.sha256(privateKey)
    val sha256Public = Crypto.sha256(publicKey)

    println("MD5(privateKey)  " + Crypto.byteArrayToHexString(md5Private))
    md5Private.length * 8 should equal(128)
    println("SHA256(privateKey)  " + Crypto.byteArrayToHexString(sha256Private))
    sha256Private.length * 8 should equal(256)

    println("MD5(publicKey)  " + Crypto.byteArrayToHexString(md5Public))
    md5Public.length * 8 should equal(128)
    println("SHA256(publicKey)  " + Crypto.byteArrayToHexString(sha256Public))
    sha256Public.length * 8 should equal(256)
  }

  "RSA Encryption Decryption Test" should "work" in {
    val pair = Crypto.generateRSAKeys()
    val privateKey = pair.getPrivate()
    val publicKey = pair.getPublic
    val secretMessage = "You found my secret!"

    val encrypted = Crypto.encryptRSA(Base64Util.encodeBinary(secretMessage), privateKey)

    println("Encrypted output:  " + Crypto.byteArrayToHexString(encrypted))

    val decrypted = Crypto.decryptRSA(encrypted, publicKey)
    val result = new String(decrypted)

    println("Decrypted output:  " + result)
    result should equal(secretMessage)
  }

  "AES Encryption Decryption Test" should "work" in {
    val key = Crypto.generateAESKey()
    val secretMessage = "You found my secret!"

    val iv = Array[Byte](0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    val encrypted = Crypto.encryptAES(Base64Util.encodeBinary(secretMessage), key, iv)

    println("Encrypted output:  " + Crypto.byteArrayToHexString(encrypted))

    val decrypted = Crypto.decryptAES(encrypted, key, iv)
    val result = new String(decrypted)

    println("Decrypted output:  " + result)
    result should equal(secretMessage)
  }

  "Base64 encode and decode" should "encode and decode a string from base64" in {
    val randomString = Random.nextString(10)

    val encoded = Base64Util.encodeString(randomString)
    val decoded = Base64Util.decodeString(encoded)

    println(decoded)
    decoded should equal(randomString)
  }

  "Encrypting and decrypting SecureObjects" should "allow encryption and decryption of all object types" in {

    for (i <- 1 to 100) {
      val pair = Crypto.generateRSAKeys()
      val aesKey = Crypto.generateAESKey()

      val baseObject = BaseObject()
      val a = Album("now", "now", -1, "desc")

      val so = Crypto.constructSecureObject(
        baseObject, 0, 0, ObjectType.album.id, a.toJson.prettyPrint, Map("0" -> pair.getPublic)
      )

      //      println(so)

      val encryptedAESKeybytes = Base64Util.decodeBinary(so.encryptedKeys("0"))
      val decryptedAESKeyBytes = Crypto.decryptRSA(encryptedAESKeybytes, pair.getPrivate)

      val reconstructKey = Crypto.constructAESKeyFromBytes(decryptedAESKeyBytes)

      reconstructKey should equal(aesKey)
      reconstructKey.getEncoded should equal(aesKey.getEncoded)

      val decryptedAlbumBytes = Crypto.decryptAES(so.data, reconstructKey, Constants.IV)

      val reconstructedAlbum = Base64Util.bytesToObj(decryptedAlbumBytes)

      //      println(reconstructedAlbum.asInstanceOf[Album])

      reconstructedAlbum should equal(a)
    }

  }


}
