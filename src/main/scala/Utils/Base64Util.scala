package Utils

/**
  * Retrieved from
  * https://github.com/wasted/scala-util/blob/master/src/main/scala/io/wasted/util/Base64.scala
  */

import org.apache.commons.codec.binary.Base64
import scala.pickling.Defaults._
import scala.pickling.binary._

object Base64Util {
  final private val b64 = new Base64

  /** Encodes the given String into a Base64 String. **/
  def encodeString(in: String): String = encodeString(in.getBytes("UTF-8"))

  /** Encodes the given ByteArray into a Base64 String. **/
  def encodeString(in: Array[Byte]): String = new String(b64.encode(in))

  /** Encodes the given String into a Base64 ByteArray. **/
  def encodeBinary(in: String): Array[Byte] = b64.encode(in.getBytes("UTF-8"))

  /** Encodes the given ByteArray into a Base64 ByteArray. **/
  def encodeBinary(in: Array[Byte]): Array[Byte] = b64.encode(in)

  /** Decodes the given Base64-ByteArray into a String. **/
  def decodeString(in: Array[Byte]): String = new String(decodeBinary(in))

  /** Decodes the given Base64-String into a String. **/
  def decodeString(in: String): String = decodeString(in.getBytes("UTF-8"))

  /** Decodes the given Base64-String into a ByteArray. **/
  def decodeBinary(in: String): Array[Byte] = decodeBinary(in.getBytes("UTF-8"))

  /** Decodes the given Base64-ByteArray into a ByteArray. **/
  def decodeBinary(in: Array[Byte]): Array[Byte] = (new Base64).decode(in)

  def objectToBytes(obj: Any): Array[Byte] = {
    obj.pickle.value
  }

  def bytesToObj(obj: Array[Byte]): Any = {
    obj.unpickle[Any]
  }

}
