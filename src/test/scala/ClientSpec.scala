import java.security.PublicKey

import Objects.ObjectJsonSupport._
import Objects.ObjectTypes.{ObjectType, PostType}
import Objects._
import Server.RootService
import Utils.{Base64Util, Constants, Crypto, Resources}
import org.joda.time.DateTime
import org.scalatest.{FreeSpec, Matchers}
import spray.http.StatusCodes._
import spray.json._
import spray.testkit.ScalatestRouteTest

import scala.util.Random

class ClientSpec extends FreeSpec with ScalatestRouteTest with Matchers with RootService {
  def actorRefFactory = system

  var serverPublicKey: PublicKey = null
  var user1Id = -1
  var user2Id = -1
  var myPageId = -1
  val user1KeyPair = Crypto.generateRSAKeys()
  val user2KeyPair = Crypto.generateRSAKeys()
  val pageKeyPair = Crypto.generateRSAKeys()

  "Get Server Key" - {
    "when calling GET /server_key" - {
      "should return the server's public key" in {
        Get("/server_key") ~> myRoute ~> check {
          status should equal(OK)
          val returnObject = responseAs[Array[Byte]]
          println(Base64Util.encodeString(returnObject))
          serverPublicKey = Crypto.constructRSAPublicKeyFromBytes(returnObject)
        }
      }
    }
  }

  "Register User1" - {
    "when calling PUT /register" - {
      "should return user id" in {
        Put("/register", user1KeyPair.getPublic.getEncoded) ~> myRoute ~> check {
          status should equal(OK)
          val secureMsg = responseAs[SecureMessage]
          secureMsg.from should equal(-1)
          val requestKeyBytes = Crypto.decryptRSA(secureMsg.encryptedKey, user1KeyPair.getPrivate)
          Crypto.verifySign(serverPublicKey, secureMsg.signature, requestKeyBytes) should equal(true)
          val requestKey = Crypto.constructAESKeyFromBytes(requestKeyBytes)
          val requestJson = Crypto.decryptAES(secureMsg.message, requestKey, Constants.IV)
          user1Id = Base64Util.decodeString(requestJson).toInt
          println(user1Id)
        }
      }
    }
  }

  "Put User1" - {
    "when calling PUT /create" - {
      "should create the new user assuming register has already happened" in {
        val fullName = Resources.names(Random.nextInt(Resources.names.length)).split(' ')
        val baseObj = new BaseObject(id = user1Id)
        val userObject = User("about", Resources.randomBirthday(), 'M', fullName(0), fullName(1), user1KeyPair.getPublic.getEncoded)
        val secureObject = Crypto.constructSecureObject(baseObj, user1Id, user1Id, ObjectType.user.id, userObject.toJson.compactPrint, Map(user1Id.toString -> user1KeyPair.getPublic))
        val secureMessage = Crypto.constructSecureMessage(user1Id, secureObject.toJson.compactPrint, serverPublicKey, user1KeyPair.getPrivate)
        Put("/create", secureMessage) ~> myRoute ~> check {
          status should equal(OK)
          println(entity)
        }
      }
    }
  }

  "Register User2" - {
    "when calling PUT /register" - {
      "should return user id" in {
        Put("/register", user2KeyPair.getPublic.getEncoded) ~> myRoute ~> check {
          status should equal(OK)
          val secureMsg = responseAs[SecureMessage]
          secureMsg.from should equal(-1)
          val requestKeyBytes = Crypto.decryptRSA(secureMsg.encryptedKey, user2KeyPair.getPrivate)
          Crypto.verifySign(serverPublicKey, secureMsg.signature, requestKeyBytes) should equal(true)
          val requestKey = Crypto.constructAESKeyFromBytes(requestKeyBytes)
          val requestJson = Crypto.decryptAES(secureMsg.message, requestKey, Constants.IV)
          user2Id = Base64Util.decodeString(requestJson).toInt
          println(user2Id)
        }
      }
    }
  }

  "Put User2" - {
    "when calling PUT /create" - {
      "should create the new user assuming register has already happened" in {
        val fullName = Resources.names(Random.nextInt(Resources.names.length)).split(' ')
        val baseObj = new BaseObject(id = user2Id)
        val userObject = User("about", Resources.randomBirthday(), 'M', fullName(0), fullName(1), user2KeyPair.getPublic.getEncoded)
        val secureObject = Crypto.constructSecureObject(baseObj, user2Id, user2Id, ObjectType.user.id, userObject.toJson.compactPrint, Map(user2Id.toString -> user2KeyPair.getPublic))
        val secureMessage = Crypto.constructSecureMessage(user2Id, secureObject.toJson.compactPrint, serverPublicKey, user2KeyPair.getPrivate)
        Put("/create", secureMessage) ~> myRoute ~> check {
          status should equal(OK)
          println(entity)
        }
      }
    }
  }

  "Register Page" - {
    "when calling PUT /register" - {
      "should return page id" in {
        Put("/register", pageKeyPair.getPublic.getEncoded) ~> myRoute ~> check {
          status should equal(OK)
          val secureMsg = responseAs[SecureMessage]
          secureMsg.from should equal(-1)
          val requestKeyBytes = Crypto.decryptRSA(secureMsg.encryptedKey, pageKeyPair.getPrivate)
          Crypto.verifySign(serverPublicKey, secureMsg.signature, requestKeyBytes) should equal(true)
          val requestKey = Crypto.constructAESKeyFromBytes(requestKeyBytes)
          val requestJson = Crypto.decryptAES(secureMsg.message, requestKey, Constants.IV)
          myPageId = Base64Util.decodeString(requestJson).toInt
          println(myPageId)
        }
      }
    }
  }

  "Put Page" - {
    "when calling PUT /create" - {
      "should create the new page assuming register has already happened" in {
        val baseObj = new BaseObject(id = myPageId)
        val pageObject = Page("about", Resources.getRandomPageCategory(), -1, pageKeyPair.getPublic.getEncoded)
        val secureObject = Crypto.constructSecureObject(baseObj, myPageId, myPageId, ObjectType.page.id, pageObject.toJson.compactPrint, Map(myPageId.toString -> pageKeyPair.getPublic))
        val secureMessage = Crypto.constructSecureMessage(myPageId, secureObject.toJson.compactPrint, serverPublicKey, pageKeyPair.getPrivate)
        Put("/create", secureMessage) ~> myRoute ~> check {
          status should equal(OK)
          println(entity)
        }
      }
    }
  }

  "User1 Requesting User2 as friend" - {
    "when calling Post /addFriend" - {
      "should add a profile to that user's friend list and allow them to view that user's content" in {
        val secureRequest = SecureRequest(user1Id, user2Id, ObjectType.user.id, -1)
        val secureMessage = Crypto.constructSecureMessage(user1Id, secureRequest.toJson.compactPrint, serverPublicKey, user1KeyPair.getPrivate)
        Post("/addfriend", secureMessage) ~> myRoute ~> check {
          status should equal(OK)
          println(entity.data.asString)
        }
      }
    }
  }

  "User2 getting friend_requests and accepting" - {
    "when calling Get /friend_requests by User2" - {
      "should return friend ID's that requested you, then addfriend those ID's" in {
        val secureRequest = SecureRequest(user2Id, user2Id, -1, -1)
        val secureMessage = Crypto.constructSecureMessage(user2Id, secureRequest.toJson.compactPrint, serverPublicKey, user2KeyPair.getPrivate)
        Get("/friend_requests", secureMessage) ~> myRoute ~> check {
          status should equal(OK)
          println(entity)

          val secureMsg = responseAs[SecureMessage]
          secureMsg.from should equal(-1)
          val requestKeyBytes = Crypto.decryptRSA(secureMsg.encryptedKey, user2KeyPair.getPrivate)
          Crypto.verifySign(serverPublicKey, secureMsg.signature, requestKeyBytes) should equal(true)
          val requestKey = Crypto.constructAESKeyFromBytes(requestKeyBytes)
          val requestJson = Base64Util.decodeString(Crypto.decryptAES(secureMsg.message, requestKey, Constants.IV))
          val requestArray = JsonParser(requestJson).convertTo[Array[Int]]

          requestArray.size should equal(1)
          println(requestArray.mkString(","))

          // request friend request
          requestArray.foreach { id =>
            val secureRequest = SecureRequest(user2Id, id, ObjectType.user.id, -1)
            val secureMessage = Crypto.constructSecureMessage(user2Id, secureRequest.toJson.compactPrint, serverPublicKey, user2KeyPair.getPrivate)
            Post("/like", secureMessage) ~> myRoute ~> check {
              status should equal(OK)
              println(entity.data.asString)
            }
          }
        }
      }
    }
  }

  "User1 getting friends_public_keys" - {
    "when calling Get /friends_public_keys by User1" - {
      "should return all public key map of friends" in {
        val secureRequest = SecureRequest(user1Id, user1Id, -1, -1)
        val secureMessage = Crypto.constructSecureMessage(user1Id, secureRequest.toJson.compactPrint, serverPublicKey, user1KeyPair.getPrivate)
        Get("/friends_public_keys", secureMessage) ~> myRoute ~> check {
          status should equal(OK)
          println(entity)

          val secureMsg = responseAs[SecureMessage]
          secureMsg.from should equal(-1)
          val requestKeyBytes = Crypto.decryptRSA(secureMsg.encryptedKey, user1KeyPair.getPrivate)
          Crypto.verifySign(serverPublicKey, secureMsg.signature, requestKeyBytes) should equal(true)
          val requestKey = Crypto.constructAESKeyFromBytes(requestKeyBytes)
          val requestJson = Base64Util.decodeString(Crypto.decryptAES(secureMsg.message, requestKey, Constants.IV))
          val requestMap = JsonParser(requestJson).convertTo[Map[String, Array[Byte]]]
          println(requestMap)
          requestMap.size should equal(1)

          requestMap.foreach { case (id, keyBytes) =>
            println(Crypto.constructRSAPublicKeyFromBytes(keyBytes))
          }
        }
      }
    }
  }



  "Put Post by User1 viewable only by this user" - {
    "when calling PUT /create" - {
      "should return a post object viewable only by this user" in {
        val baseObject = new BaseObject()
        val postObject = Objects.Post(new DateTime().toString, Resources.getRandomStatus(), PostType.status, -1)
        val secureObject = Crypto.constructSecureObject(baseObject, user1Id, user1Id, ObjectType.post.id, postObject.toJson.compactPrint, Map(user1Id.toString -> user1KeyPair.getPublic))
        val secureMessage = Crypto.constructSecureMessage(user1Id, secureObject.toJson.compactPrint, serverPublicKey, user1KeyPair.getPrivate)

        Put("/create", secureMessage) ~> myRoute ~> check {
          status should equal(OK)
          println(entity)

          // TODO do 2 gets here, one this succeeds because they are authorized, the next that fails
        }
      }
    }
  }

  "Put Post by User1 trying to pose as User2" - {
    "when calling PUT /create" - {
      "should FAIL, return defaultResponse" in {
        val baseObject = new BaseObject()
        val postObject = Objects.Post(new DateTime().toString, Resources.getRandomStatus(), PostType.status, -1)
        // FROM in secureMessage != from in secureObject
        val secureObject = Crypto.constructSecureObject(baseObject, user2Id, user1Id, ObjectType.post.id, postObject.toJson.compactPrint,
          Map(user1Id.toString -> user1KeyPair.getPublic, user2Id.toString -> user2KeyPair.getPublic, myPageId.toString -> pageKeyPair.getPublic))
        val secureMessage = Crypto.constructSecureMessage(user1Id, secureObject.toJson.compactPrint, serverPublicKey, user1KeyPair.getPrivate)

        Put("/create", secureMessage) ~> myRoute ~> check {
          status should equal(OK)
          println(entity)

          // TODO do 2 gets here, both should succeed
        }
      }
    }
  }

  "Put Post by User1 viewable by all others" - {
    "when calling PUT /create" - {
      "should FAIL, return defaultResponse" in {
        val baseObject = new BaseObject()
        val postObject = Objects.Post(new DateTime().toString, Resources.getRandomStatus(), PostType.status, -1)
        val secureObject = Crypto.constructSecureObject(baseObject, user1Id, user1Id, ObjectType.post.id, postObject.toJson.compactPrint,
          Map(user1Id.toString -> user1KeyPair.getPublic, user2Id.toString -> user2KeyPair.getPublic, myPageId.toString -> pageKeyPair.getPublic))
        val secureMessage = Crypto.constructSecureMessage(user1Id, secureObject.toJson.compactPrint, serverPublicKey, user1KeyPair.getPrivate)

        Put("/create", secureMessage) ~> myRoute ~> check {
          status should equal(OK)
          println(entity)

          // TODO do 2 gets here, both should succeed
        }
      }
    }
  }

  "Put Post by Page" - {
    "when calling PUT /create" - {
      "should return a post object viewable by everyone" in {
        val baseObject = new BaseObject()
        val postObject = Objects.Post(new DateTime().toString, Resources.getRandomStatus(), PostType.status, -1)
        val secureObject = Crypto.constructSecureObject(baseObject, myPageId, myPageId, ObjectType.post.id, postObject.toJson.compactPrint, Map(myPageId.toString -> pageKeyPair.getPublic))
        val secureMessage = Crypto.constructSecureMessage(myPageId, secureObject.toJson.compactPrint, serverPublicKey, pageKeyPair.getPrivate)

        Put("/create", secureMessage) ~> myRoute ~> check {
          status should equal(OK)
          println(entity)
        }
      }
    }
  }

  "Put Picture by User2 viewable only by this user" - {
    "when calling PUT /create" - {
      "should return a picture object viewable only by this user" in {
        val baseObject = BaseObject()
        val pictureObject = Picture("filename.png", "")
        val secureObject = Crypto.constructSecureObject(baseObject, user2Id, user2Id, ObjectType.post.id, pictureObject.toJson.compactPrint, Map(user2Id.toString -> user2KeyPair.getPublic))
        val secureMessage = Crypto.constructSecureMessage(user2Id, secureObject.toJson.compactPrint, serverPublicKey, user2KeyPair.getPrivate)
        Put("/create", secureMessage) ~> myRoute ~> check {
          status should equal(OK)
          println(entity)
          // TODO do 2 gets here, one this succeeds because they are authorized, the next that fails
        }
      }
    }
  }

  "Put Album by User2 viewable only by this user" - {
    "when calling PUT /create" - {
      "should return a album object viewable by only this user" in {
        val baseObject = BaseObject()
        val albumObject = Album(new DateTime().toString, new DateTime().toString, -1, "desc")
        val secureObject = Crypto.constructSecureObject(baseObject, user2Id, user2Id, ObjectType.post.id, albumObject.toJson.compactPrint, Map(user2Id.toString -> user2KeyPair.getPublic))
        val secureMessage = Crypto.constructSecureMessage(user2Id, secureObject.toJson.compactPrint, serverPublicKey, user2KeyPair.getPrivate)
        Put("/create", secureMessage) ~> myRoute ~> check {
          status should equal(OK)
          println(entity)
          // TODO do 2 gets here, one this succeeds because they are authorized, the next that fails
        }

      }
    }
  }

  "Post Page by page" - {
    "when calling POST /update" - {
      "should update Page object" in {
        val baseObject = BaseObject(id = myPageId)
        val pageObject = Page("about", Resources.getRandomPageCategory(), -1, pageKeyPair.getPublic.getEncoded)
        val secureObject = Crypto.constructSecureObject(baseObject, myPageId, myPageId, ObjectType.page.id, pageObject.toJson.compactPrint, Map(myPageId.toString -> pageKeyPair.getPublic))
        val secureMessage = Crypto.constructSecureMessage(myPageId, secureObject.toJson.compactPrint, serverPublicKey, pageKeyPair.getPrivate)
        Post("/update", secureMessage) ~> myRoute ~> check {
          status should equal(OK)
          println(entity)
        }
      }
    }
  }

  //  "Post Picture by User2" - {
  //    "when calling POST /picture" - {
  //      "should update Picture object" in {
  //        val pictureObject = Picture(BaseObject(), user2Id, -1, "filename.png", "")
  //        val secureObject = Crypto.constructSecureObject(pictureObject.baseObject, ObjectType.post.id, pictureObject.toJson.compactPrint, Map(user2Id.toString -> user2KeyPair.getPublic))
  //        val secureMessage = Crypto.constructSecureMessage(user2Id, secureObject.toJson.compactPrint, serverKey, user2KeyPair.getPrivate)
  //        Post("/picture", secureMessage) ~> myRoute ~> check{
  //          status should equal(OK)
  //          println(entity)
  //          // TODO do 2 gets here, one this succeeds because they are authorized, the next that fails
  //        }
  //      }
  //    }
  //  }
  //
  //  "Post Album" - {
  //    "when calling Post /album" - {
  //      "should return a album object" in {
  //
  //      }
  //    }
  //  }
  //
  //  "Create friendship between 0 and 1" - {
  //    "when calling Post /friendlist" - {
  //      "should return an UpdateFriendlist object" in {
  //
  //      }
  //    }
  //  }

  "Get User1 public_key" - {
    "when getting User1 public_key" - {
      "should return User1 public_key" in {
        Get("/getpublickey/" + user1Id) ~> myRoute ~> check {
          status should equal(OK)
          println(entity)
          val data = responseAs[Array[Byte]]
          val publicKey = Crypto.constructRSAPublicKeyFromBytes(data)
          println(publicKey)
        }
      }
    }
  }


  "Get User1 profile by User1" - {
    "when getting User1" - {
      "should return User1 object" in {
        val secureRequest = SecureRequest(user1Id, user1Id, ObjectType.user.id, user1Id)
        val secureMessage = Crypto.constructSecureMessage(user1Id, secureRequest.toJson.compactPrint, serverPublicKey, user1KeyPair.getPrivate)
        Get("/request", secureMessage) ~> myRoute ~> check {
          status should equal(OK)
          println(entity)
          val secureMsg = responseAs[SecureMessage]
          val requestKeyBytes = Crypto.decryptRSA(secureMsg.encryptedKey, user1KeyPair.getPrivate)
          Crypto.verifySign(serverPublicKey, secureMsg.signature, requestKeyBytes) should equal(true)
          val json = Base64Util.decodeString(
            Crypto.decryptAES(secureMsg.message, Crypto.constructAESKeyFromBytes(requestKeyBytes), Constants.IV)
          )
          val secureObject = JsonParser(json).convertTo[SecureObject]

          println(secureObject.baseObj.likes)
          val aesKey = Crypto.constructAESKeyFromBytes(Crypto.decryptRSA(secureObject.encryptedKeys(user1Id.toString), user1KeyPair.getPrivate))

          val userJson = Base64Util.decodeString(Crypto.decryptAES(secureObject.data, aesKey, Constants.IV))

          val userObj = JsonParser(userJson).convertTo[User]

          println(userObj)


        }
      }
    }
  }

  //  "Get feed for 0" - {
  //    "when getting feed for user 0" - {
  //      "should return latest post object" in {
  //
  //
  //      }
  //    }
  //  }


  "Get post 3 for User1 from User2" - {
    "when getting post 3 for User1" - {
      "should return Post id=3 object" in {
        Thread.sleep(1000)
        val secureRequest = SecureRequest(user2Id, user1Id, ObjectType.post.id, 3)
        val secureMessage = Crypto.constructSecureMessage(user2Id, secureRequest.toJson.compactPrint, serverPublicKey, user2KeyPair.getPrivate)
        Get("/request", secureMessage) ~> myRoute ~> check {
          status should equal(OK)
          println(entity)
          val secureMsg = responseAs[SecureMessage]
          val requestKeyBytes = Crypto.decryptRSA(secureMsg.encryptedKey, user2KeyPair.getPrivate)
          Crypto.verifySign(serverPublicKey, secureMsg.signature, requestKeyBytes) should equal(true)
          val json = Base64Util.decodeString(
            Crypto.decryptAES(secureMsg.message, Crypto.constructAESKeyFromBytes(requestKeyBytes), Constants.IV)
          )
          val secureObject = JsonParser(json).convertTo[SecureObject]

          val aesKey = Crypto.constructAESKeyFromBytes(Crypto.decryptRSA(secureObject.encryptedKeys(user2Id.toString), user2KeyPair.getPrivate))

          val userJson = Base64Util.decodeString(Crypto.decryptAES(secureObject.data, aesKey, Constants.IV))

          val postObj = JsonParser(userJson).convertTo[Post]

          println(postObj)
        }
      }
    }
  }


  "Post User1" - {
    "when calling POST /update" - {
      "should update User object" in {
        val baseObject = new BaseObject(id = user1Id)
        val fullName = Resources.names(Random.nextInt(Resources.names.length)).split(' ')
        val userObject = User("about", Resources.randomBirthday(), 'M', fullName(0), fullName(1), user1KeyPair.getPublic.getEncoded)
        val secureObject = Crypto.constructSecureObject(baseObject, user1Id, user1Id, ObjectType.user.id, userObject.toJson.compactPrint, Map(user1Id.toString -> user1KeyPair.getPublic))
        val secureMessage = Crypto.constructSecureMessage(user1Id, secureObject.toJson.compactPrint, serverPublicKey, user1KeyPair.getPrivate)
        Post("/update", secureMessage) ~> myRoute ~> check {
          status should equal(OK)
          println(entity)
        }
      }
    }
  }


  "Get post 3 for User2 from User1 who is authorized, but the post does not have User1 version of key" - {
    "when getting post 3 for User2" - {
      "should return Post id=3 object" in {
        Thread.sleep(1000)
        val secureRequest = SecureRequest(user1Id, user2Id, ObjectType.post.id, 3)
        val secureMessage = Crypto.constructSecureMessage(user1Id, secureRequest.toJson.compactPrint, serverPublicKey, user1KeyPair.getPrivate)
        Get("/request", secureMessage) ~> myRoute ~> check {
          status should equal(OK)
          println(entity)
          val secureMsg = responseAs[SecureMessage]
          val requestKeyBytes = Crypto.decryptRSA(secureMsg.encryptedKey, user1KeyPair.getPrivate)
          Crypto.verifySign(serverPublicKey, secureMsg.signature, requestKeyBytes) should equal(true)
          val json = Base64Util.decodeString(
            Crypto.decryptAES(secureMsg.message, Crypto.constructAESKeyFromBytes(requestKeyBytes), Constants.IV)
          )
          val secureObject = JsonParser(json).convertTo[SecureObject]

          try {
            val aesKey = Crypto.constructAESKeyFromBytes(Crypto.decryptRSA(secureObject.encryptedKeys(user1Id.toString), user1KeyPair.getPrivate))

            val userJson = Base64Util.decodeString(Crypto.decryptAES(secureObject.data, aesKey, Constants.IV))

            val postObj = JsonParser(userJson).convertTo[Post]

            println(postObj)
          } catch {
            case e: Throwable =>
          }
        }
      }
    }
  }


  "Get post 3 for User1 from User2, who now is unauthorized" - {
    "when getting post 3 for User1" - {
      "should return Post id=3 object" in {
        Thread.sleep(1000)
        val secureRequest = SecureRequest(user2Id, user1Id, ObjectType.post.id, 3)
        val secureMessage = Crypto.constructSecureMessage(user2Id, secureRequest.toJson.compactPrint, serverPublicKey, user2KeyPair.getPrivate)
        Get("/request", secureMessage) ~> myRoute ~> check {
          status should equal(OK)
          println(entity)
          val secureMsg = responseAs[SecureMessage]
          val requestKeyBytes = Crypto.decryptRSA(secureMsg.encryptedKey, user2KeyPair.getPrivate)
          Crypto.verifySign(serverPublicKey, secureMsg.signature, requestKeyBytes) should equal(true)
          val json = Base64Util.decodeString(
            Crypto.decryptAES(secureMsg.message, Crypto.constructAESKeyFromBytes(requestKeyBytes), Constants.IV)
          )
          json should equal("Unauthorized Request! Not Request!")
        }
      }
    }
  }


  //  "Get album 0 for 0" - {
  //    "when getting album 0 for user 0" - {
  //      "should return album 0 object" in {
  //
  //      }
  //    }
  //  }
  //  "Get picture 1 for profile 0" - {
  //    "when getting picture 1 for user 0" - {
  //      "should return picture 1 object" in {
  //
  //      }
  //    }
  //  }
  //
  //  "1 likes 0's post" - {
  //    "when calling PUT /like for a post" - {
  //      "should return a post object" in {
  //
  //      }
  //    }
  //  }

  "Delete User1's Post3 by User1" - {
    "when calling DELETE /delete" - {
      "should return a post object" in {
        val secureRequest = SecureRequest(user1Id, user1Id, ObjectType.post.id, 3)
        val secureMessage = Crypto.constructSecureMessage(user1Id, secureRequest.toJson.compactPrint, serverPublicKey, user1KeyPair.getPrivate)
        Delete("/delete", secureMessage) ~> myRoute ~> check {
          status should equal(OK)
          println(entity)
        }
      }
    }
  }

  //  "Delete Picture" - {
  //    "when calling DELETE /picture" - {
  //      "should return a picture object" in {
  //
  //      }
  //    }
  //  }
  //
  //  "Delete Album" - {
  //    "when calling DELETE /album" - {
  //      "should return a album object" in {
  //
  //      }
  //    }
  //  }
  //
  //  "Delete User" - {
  //    "when calling DELETE /user" - {
  //      "should return a user object each" in {
  //
  //      }
  //    }
  //  }
  //
  //  "Delete Page" - {
  //    "when calling DELETE /page" - {
  //      "should return a page object" in {
  //
  //      }
  //    }
  //  }
}
