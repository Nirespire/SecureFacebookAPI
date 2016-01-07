package Utils

import java.security.PublicKey

import scala.collection.mutable

object Constants {
  val putProfilesChar = 'P'
  val putPostsChar = 'O'
  val putAlbumsChar = 'A'
  val putPicturesChar = 'I'

  val postFlChar = 'F'
  val postUserChar = 'z'
  val postPageChar = 'x'
  val postPostChar = 'c'
  val postPictureChar = 'v'
  val postAlbumChar = 'b'

  val getProfilesChar = 'p'
  val getPostsChar = 'o'
  val getPicturesChar = 'i'
  val getAlbumsChar = 'a'
  val getFlChar = 'f'
  val getFeedChar = 'e'

  val deleteUserChar = '1'
  val deletePageChar = '2'
  val deletePostChar = '3'
  val deletePictureChar = '4'
  val deleteAlbumChar = '5'

  val likeChar = 'l'
  val postLikeChar = 'q'

  val registerChar = 'X'

  val nano = 1000000000.0
  val trueBool = true
  val falseBool = false

  val IV = Array[Byte](1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)

  val randomStringHeader = "RandomString"
  val signedStringHeader = "SignedString"
  val authTokenHeader = "AuthToken"
  val serverPublicKeyHeader = "ServerPublicKey"

  val serverId = -1
  val defaultKeyPair = Crypto.generateRSAKeys()
  val defaultPublicKey = defaultKeyPair.getPublic
  val defaultPrivateKey = defaultKeyPair.getPrivate
  val defaultKey = Crypto.generateAESKey()
  val serverKeyPair = Crypto.generateRSAKeys()
  val serverPublicKey = serverKeyPair.getPublic
  val serverPrivateKey = serverKeyPair.getPrivate

  val userPublicKeys = mutable.HashMap[Int, PublicKey]()
}