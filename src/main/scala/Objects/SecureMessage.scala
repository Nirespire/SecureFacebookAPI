package Objects

case class SecureMessage(
                          from: Int,
                          message: Array[Byte],
                          signature: Array[Byte],
                          encryptedKey: Array[Byte]
                        )