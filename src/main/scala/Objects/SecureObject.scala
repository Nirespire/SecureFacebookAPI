package Objects

import Utils.Base64Util

case class SecureObject(
                         baseObj: BaseObject,
                         from: Int,
                         to: Int,
                         objectType: Int,
                         data: Array[Byte],
                         encryptedKeys: Map[String, Array[Byte]]
                       ) {
  override def toString(): String = {
    val sb = new StringBuilder()
    sb.append(s"Object created by $from for $to,\n")
    sb.append(s"Object : $baseObj, ObjectType: $objectType,\n")
    sb.append(s"Base64Content: ${Base64Util.encodeString(data)}, \n")
    sb.append(s"Encrypted Key: ${Base64Util.encodeString(encryptedKeys.mkString(","))}\n")
    sb.toString()
  }
}