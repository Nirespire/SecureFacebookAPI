package Client

import java.security.PublicKey
import javax.crypto.BadPaddingException
import Client.ClientType.ClientType
import Client.Messages._
import Objects.ObjectTypes.ObjectType
import Objects.ObjectTypes.ObjectType
import Objects.ObjectTypes.ObjectType.ObjectType
import Utils.Resources._
import Objects.ObjectJsonSupport._
import Objects.ObjectTypes.PostType._
import Objects._
import Utils.{Resources, Base64Util, Crypto, Constants}
import akka.actor.{Actor, ActorLogging, ActorRef}
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import spray.client.pipelining
import spray.client.pipelining._
import spray.http.HttpResponse
import spray.http.StatusCodes._
import spray.json.JsonParser.ParsingException
import spray.json._

import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success, Try}


class ClientActor(isPage: Boolean = false, clientType: ClientType) extends Actor with ActorLogging {
  val myPages = mutable.ArrayBuffer[Page]()
  val myFriends = mutable.ArrayBuffer[Int]()
  val myRealFriends = mutable.HashMap[ActorRef, Int]()
  var myFriendsPublicKeys = Map[String, PublicKey]()
  val waitForIdFriends = mutable.Set[ActorRef]()
  val returnHandshake = mutable.Set[ActorRef]()
  var me: User = null
  var mePage: Page = null
  var myBaseObj: BaseObject = null
  var numPosts = 0
  var numAlbums = 0
  var numPictures = 0

  private val keyPair = Crypto.generateRSAKeys()
  var serverPublicKey: PublicKey = null

  val (putPercent, getPercent, friendPercent, updatePercent) = clientType match {
    case ClientType.Active => (80, 50, 90, 50)
    case ClientType.Passive => (20, 90, 80, 5)
    case ClientType.ContentCreator => (70, 20, 10, 40)
  }

  val config = ConfigFactory.load()
  lazy val servicePort = Try(config.getInt("service.port")).getOrElse(8080)
  lazy val serviceHost = Try(config.getString("service.host")).getOrElse("localhost")

  import context.dispatcher

  def random(n: Int) = Random.nextInt(n)

  def durationSeconds(n: Int) = n.seconds

  def randomDuration(n: Int) = durationSeconds(random(n))

  def receive = {
    // Create a user profile or page for self
    case true => registerMyself()
    case false if !myBaseObj.deleted && !isPage => updateFriendPublicKeys()
    case Activity if !myBaseObj.deleted => activity()

    case MakePost(postType, attachmentID) =>
      val newPost = Objects.Post(new DateTime().toString(), statuses(Random.nextInt(statuses.length)), postType, attachmentID)
      put(createSecureObjectMessage(newPost, myBaseObj.id, myBaseObj.id, ObjectType.post, myFriendsPublicKeys), "create", "post")

    case MakePicturePost =>
      val newPicture = Picture("filename", "")
      put(createSecureObjectMessage(newPicture, myBaseObj.id, myBaseObj.id, ObjectType.picture, myFriendsPublicKeys), "create", "picturepost")

    case MakePicture(albumID) =>
      val newPicture = Picture("filename", "")
      put(createSecureObjectMessage(newPicture, myBaseObj.id, myBaseObj.id, ObjectType.picture, myFriendsPublicKeys), "create", "picture")

    case AddPictureToAlbum =>

    case MakeAlbum =>
      val newAlbum = Album(new DateTime().toString, new DateTime().toString, -1, "album desc")
      put(createSecureObjectMessage(newAlbum, myBaseObj.id, myBaseObj.id, ObjectType.album, myFriendsPublicKeys), "create", "album")

    case UpdatePost(postType, attachment) =>
      val newPost = Objects.Post(new DateTime().toString, Resources.getRandomStatus(), postType, attachment)
      post(createSecureObjectMessage(newPost, myBaseObj.id, myBaseObj.id, ObjectType.post, myFriendsPublicKeys, random(numPosts) + 2), "update", "post")

    case UpdatePicture =>
      val newPicture = Picture("filename", "")
      post(createSecureObjectMessage(newPicture, myBaseObj.id, myBaseObj.id, ObjectType.picture, myFriendsPublicKeys, random(numPictures) + 2), "update", "picture")

    case UpdateAlbum(cover) =>
      val newAlbum = Album(new DateTime().toString, new DateTime().toString, cover, "album desc")
      post(createSecureObjectMessage(newAlbum, myBaseObj.id, myBaseObj.id, ObjectType.album, myFriendsPublicKeys, random(numAlbums) + 2), "update", "album")

    // From matchmaker
    case aNewFriend: ActorRef =>
      if (myBaseObj == null) {
        waitForIdFriends.add(aNewFriend)
      } else {
        //        log.info(myBaseObj.id + " just met someone")
        aNewFriend ! Handshake(Constants.trueBool, myBaseObj.id)
      }
    // From new friend
    case Handshake(needResponse, id) =>
      if (myBaseObj == null) {
        returnHandshake.add(sender)
      } else {
        myRealFriends.put(sender, id)
        if (needResponse) {
          self ! RequestFriend(id)
          sender ! Handshake(Constants.falseBool, myBaseObj.id)
        }
      }
    case RequestFriend(id) =>
      val secureMessage = createSecureRequestMessage(myBaseObj.id, id, ObjectType.user, id)
      post(secureMessage, "addfriend")

    case AcceptFriendRequests =>
      val secureMessage = createSecureRequestMessage(myBaseObj.id, myBaseObj.id, ObjectType.user, -1)
      get(secureMessage, "friend_requests")

    case PutMsg(response, reaction) => handlePutResponse(response, reaction)
    case GetMsg(response, reaction) => handleGetResponse(response, reaction)
    case PostMsg(response, reaction) =>
      try {
        reaction match {
          case "addfriend" =>
            log.info(response ~> unmarshal[String])
          case "acceptfriendrequest" =>
            log.info(response ~> unmarshal[String])
          case _ => //log.info(s"Updated $reaction")
        }
      } catch {
        case e: Throwable => log.error(e, "Error for response {}", response)
      }

    case DebugMsg => get(null, "debug")

    case DeleteMsg(response, reaction) =>
      reaction match {
        case "user" | "page" => myBaseObj.deleted = true
        case _ => //log.info(s"${myBaseObj.id} - $reaction")
      }
  }

  def registerMyself() = {
    val pipeline = sendReceive

    val future = pipeline {
      pipelining.Get(s"http://$serviceHost:$servicePort/server_key")
    }

    future onComplete {
      case Success(response) =>
        val returnBytes = response ~> unmarshal[Array[Byte]]
        serverPublicKey = Crypto.constructRSAPublicKeyFromBytes(returnBytes)

        val future2 = pipeline {
          pipelining.Put(s"http://$serviceHost:$servicePort/register", keyPair.getPublic.getEncoded)
        }

        future2 onComplete {
          case Success(response) =>
            val secureMsg = response ~> unmarshal[SecureMessage]
            val requestKeyBytes = Crypto.decryptRSA(secureMsg.encryptedKey, keyPair.getPrivate)
            if (Crypto.verifySign(serverPublicKey, secureMsg.signature, requestKeyBytes)) {
              val requestKey = Crypto.constructAESKeyFromBytes(requestKeyBytes)
              val requestJson = Crypto.decryptAES(secureMsg.message, requestKey, Constants.IV)
              myBaseObj = BaseObject(id = Base64Util.decodeString(requestJson).toInt)


              if (isPage) {
                mePage = Page("about", Resources.getRandomPageCategory(), -1, keyPair.getPublic.getEncoded)
                val secureObject = Crypto.constructSecureObject(myBaseObj, myBaseObj.id, myBaseObj.id, ObjectType.page.id, mePage.toJson.compactPrint, Map(myBaseObj.id.toString -> keyPair.getPublic))
                val secureMessage = Crypto.constructSecureMessage(myBaseObj.id, secureObject.toJson.compactPrint, serverPublicKey, keyPair.getPrivate)
                val future3 = pipeline {
                  pipelining.Put(s"http://$serviceHost:$servicePort/create", secureMessage)
                }

                future3 onComplete {
                  case Success(response) =>
                    //                    log.info(response.toString)
                    self ! Activity()
                  case Failure(error) => log.error(error, s"Couldn't put Page")
                }
              }

              else {
                val fullName = Resources.names(Random.nextInt(Resources.names.length)).split(' ')
                me = User("about", Resources.randomBirthday(), 'M', fullName(0), fullName(1), keyPair.getPublic.getEncoded)
                val secureObject = Crypto.constructSecureObject(myBaseObj, myBaseObj.id, myBaseObj.id, ObjectType.user.id, me.toJson.compactPrint, Map(myBaseObj.id.toString -> keyPair.getPublic))
                val secureMessage = Crypto.constructSecureMessage(myBaseObj.id, secureObject.toJson.compactPrint, serverPublicKey, keyPair.getPrivate)
                val future3 = pipeline {
                  pipelining.Put(s"http://$serviceHost:$servicePort/create", secureMessage)
                }

                future3 onComplete {
                  case Success(response) =>
                    //                    log.info(response.toString)
                    self ! false
                  case Failure(error) => log.error(error, s"Couldn't put User")
                }
              }
            }
          case Failure(error) => log.error(error, s"Couldn't register")
        }
      case Failure(error) => log.error(error, s"Couldn't get server_key")
    }
  }

  def updateFriendPublicKeys() = {
    val pipeline = sendReceive

    val secureMsg = createSecureRequestMessage(myBaseObj.id, myBaseObj.id, ObjectType.user, -1)
    val future = pipeline {
      pipelining.Get(s"http://$serviceHost:$servicePort/friends_public_keys", secureMsg)
    }

    future onComplete {
      case Success(response) =>
        val requestIds = decryptSecureRequestMessage(response ~> unmarshal[SecureMessage], ObjectType.userKeyMap)
        myFriendsPublicKeys = requestIds.asInstanceOf[Map[String, Array[Byte]]].map { case (id, bytes) => (id, Crypto.constructRSAPublicKeyFromBytes(bytes)) } + (myBaseObj.id.toString -> keyPair.getPublic)
        self ! Activity

      case Failure(error) => log.error(error, s"Couldn't get pending requests")
    }
  }


  def putRoute(route: String, inputReaction: String = ""): Unit = {
    val reaction = if (inputReaction.nonEmpty) inputReaction else route
    val pipeline = sendReceive
    val future = pipeline {
      pipelining.Put(s"http://$serviceHost:$servicePort/$route")
    }

    future onComplete {
      case Success(response) => self ! PutMsg(response, reaction)
      case Failure(error) => log.error(error, s"Couldn't create using $route")
    }
  }

  def put(json: SecureMessage, route: String, inputReaction: String = ""): Unit = {
    val reaction = if (inputReaction.nonEmpty) inputReaction else route
    val pipeline = sendReceive
    val future = pipeline {
      pipelining.Put(s"http://$serviceHost:$servicePort/$route", json)
    }

    future onComplete {
      case Success(response) => self ! PutMsg(response, reaction)
      case Failure(error) => log.error(error, s"Couldn't create $json using $route")
    }
  }

  def get(json: SecureMessage, route: String, inputReaction: String = "") = {
    val reaction = if (inputReaction.nonEmpty) inputReaction else route
    val pipeline = sendReceive
    val future = pipeline {
      pipelining.Get(s"http://$serviceHost:$servicePort/$route", json)
    }

    future onComplete {
      case Success(response) => self ! GetMsg(response, reaction)
      case Failure(error) => log.error(error, s"Couldn't get $route")
    }
  }

  def post(json: SecureMessage, route: String, inputReaction: String = "") = {
    val reaction = if (inputReaction.nonEmpty) inputReaction else route
    val pipeline = sendReceive
    val future = pipeline {
      pipelining.Post(s"http://$serviceHost:$servicePort/$route", json)
    }

    future onComplete {
      case Success(response) => self ! PostMsg(response, reaction)
      case Failure(error) => log.error(error, s"Couldn't post $json using $route")
    }
  }

  def delete(json: SecureMessage, route: String, inputReaction: String = "") = {
    val reaction = if (inputReaction.nonEmpty) inputReaction else route
    val pipeline = sendReceive
    val future = pipeline {
      pipelining.Delete(s"http://$serviceHost:$servicePort/$route", json)
    }

    future onComplete {
      case Success(response) => self ! DeleteMsg(response, reaction)
      case Failure(error) => log.error(error, s"Couldn't post $json using $route")
    }
  }

  def activity() = {
    //    log.info(myBaseObj.id + " starting activity")

    context.system.scheduler.scheduleOnce(randomDuration(10), self, AcceptFriendRequests)

    if (isPage) {
      val secureMsg = createSecureRequestMessage(myBaseObj.id, myBaseObj.id, ObjectType.page, myBaseObj.id)
      get(secureMsg, "request", "page")
    }
    else {
      val secureMsg = createSecureRequestMessage(myBaseObj.id, myBaseObj.id, ObjectType.user, myBaseObj.id)
      get(secureMsg, "request", "user")
    }

    if (random(1001) <= 5) {
      random(3) match {
        case 0 =>
          if (numPosts > 0) {
            val delMsg = createSecureRequestMessage(myBaseObj.id, myBaseObj.id, ObjectType.post, random(numPosts) + 2)
            delete(delMsg, "delete", "postdelete")
          }
        case 1 =>
          if (numAlbums > 0) {
            val delMsg = createSecureRequestMessage(myBaseObj.id, myBaseObj.id, ObjectType.album, random(numAlbums) + 2)
            delete(delMsg, "delete", "albumdelete")
          }
        case 2 =>
          if (numPictures > 0) {
            val delMsg = createSecureRequestMessage(myBaseObj.id, myBaseObj.id, ObjectType.picture, random(numPictures) + 2)
            delete(delMsg, "delete", "picturedelete")
          }
      }
    }

    // Create content
    if (random(101) < putPercent) {
      random(4) match {
        case 0 => context.system.scheduler.scheduleOnce(randomDuration(3), self, MakePost(status, -1))
        case 1 => context.system.scheduler.scheduleOnce(randomDuration(3), self, MakeAlbum)
        case 2 => context.system.scheduler.scheduleOnce(randomDuration(3), self, MakePicture(-1))
        case 3 => context.system.scheduler.scheduleOnce(randomDuration(3), self, MakePicturePost)
      }
    }

    // Get friends' content
    if (random(101) < getPercent) {
      random(2) match {
        case 0 =>
          myRealFriends.foreach {
            case (ref: ActorRef, id: Int) =>
              val getFeedRequest = createSecureRequestMessage(myBaseObj.id, id, ObjectType.post, -1)
              get(getFeedRequest, "feed")

          }
        case 1 =>
          myFriendsPublicKeys.foreach {
            case (id: String, key: PublicKey) =>
              val getFeedRequest = createSecureRequestMessage(myBaseObj.id, id.toInt, ObjectType.post, -1)
              get(getFeedRequest, "feed")
          }
      }
    }

    // update your own content
    if (random(101) < updatePercent) {
      random(3) match {
        case 0 =>
          if (numPosts > 1) {
            context.system.scheduler.scheduleOnce(randomDuration(3), self, UpdatePost(status, -1))
          }
        case 1 =>
          if (numPictures > 1) {
            context.system.scheduler.scheduleOnce(randomDuration(3), self, UpdatePicture)
          }
        case 2 =>
          if (numAlbums > 1 && numPictures > 1) {
            context.system.scheduler.scheduleOnce(randomDuration(3), self, UpdateAlbum(random(numPictures) + 2))
          }
      }
    }

    context.system.scheduler.scheduleOnce(randomDuration(3), self, Constants.falseBool)

    // Delete self case
    if (random(100001) < 5) {
      if (isPage) {
        val delMsg = createSecureRequestMessage(myBaseObj.id, myBaseObj.id, ObjectType.page, myBaseObj.id)
        delete(delMsg, "delete", "pagedelete")
      }
      else {
        val delMsg = createSecureRequestMessage(myBaseObj.id, myBaseObj.id, ObjectType.user, myBaseObj.id)
        delete(delMsg, "delete", "userdelete")
      }
    }
  }

  def handlePutResponse(response: HttpResponse, reaction: String) = {
    val updateRequest = random(101) < updatePercent

    reaction match {
      case "registerUser" =>

      case "registerPage" =>


      case "user" | "page" =>
        //        if (reaction == "user") {
        //          me = response ~> unmarshal[User]
        //          myBaseObj = me.baseObject
        //
        //        } else {
        //          mePage = response ~> unmarshal[Page]
        //          myBaseObj = mePage.baseObject
        //        }

        ProfileMap.obj.put(myBaseObj.id, isPage)
        waitForIdFriends.foreach(f => self ! f)
        waitForIdFriends.clear()
        returnHandshake.foreach(f => self.tell(Handshake(Constants.trueBool, myBaseObj.id), f))
        returnHandshake.clear()
        //          log.info(s"Printing $me - $myBaseObj")
        self ! Constants.falseBool
        if (myBaseObj.id == 0) get(null, "debug")
        if (updateRequest) post(response.entity.asString.parseJson.asInstanceOf[SecureMessage], "profile")
      case "post" =>
        numPosts += 1
      case "album" =>
        numAlbums += 1
      case "picturepost" =>
        numPictures += 1
      case "picture" =>
        numPictures += 1
      case "likepage" =>
      case "like" =>
        log.info(response ~> unmarshal[String])
    }
  }

  def handleGetResponse(response: HttpResponse, reaction: String) = {
    reaction match {
      case "postdelete" => delete(response.entity.asString.parseJson.asInstanceOf[SecureMessage], "post")
      case "albumdelete" => delete(response.entity.asString.parseJson.asInstanceOf[SecureMessage], "album")
      case "picturedelete" => delete(response.entity.asString.parseJson.asInstanceOf[SecureMessage], "picture")
      case "debug" =>
        log.info(s"${response.entity.asString}")
        context.system.scheduler.scheduleOnce(durationSeconds(2), self, DebugMsg)
      case "user" =>
        decryptSecureObjectMessage(response ~> unmarshal[SecureMessage], ObjectType.user)
      case "page" =>
        decryptSecureObjectMessage(response ~> unmarshal[SecureMessage], ObjectType.page)
      case "friendpublickeys" =>
      case "friend_requests" =>
        val idList = decryptSecureRequestMessage(response ~> unmarshal[SecureMessage], ObjectType.intArray).asInstanceOf[Array[Int]]
        idList.foreach { id =>
          val likeMessage = createSecureRequestMessage(myBaseObj.id, id, ObjectType.user, id)
          post(likeMessage, "like", "acceptfriendrequest")
        }
      case "feed" =>
        val secureObjectList = decryptSecureRequestMessage(response ~> unmarshal[SecureMessage], ObjectType.secureObjectArray).asInstanceOf[Array[SecureObject]]
        if (!secureObjectList.isEmpty) {
//          log.info(s"${myBaseObj.id} feed ${secureObjectList.size}")
          secureObjectList.foreach { case so =>
            decryptSecureObject(so, ObjectType.post)
          }
        }
      case "feedpost" =>
      case "picture" =>
        decryptSecureObjectMessage(response ~> unmarshal[SecureMessage], ObjectType.picture)
      case "post" =>
        decryptSecureObjectMessage(response ~> unmarshal[SecureMessage], ObjectType.post)
      case "getalbumaddpicture" =>
      case "album" =>
        decryptSecureObjectMessage(response ~> unmarshal[SecureMessage], ObjectType.album)
      case x => log.error("Unmatched getmsg case {}", x)
    }
  }

  def createSecureObjectMessage(obj: Any, from: Int, to: Int, objType: ObjectType, keys: Map[String, PublicKey], id: Int = -1): SecureMessage = {
    objType match {
      case ObjectType.post =>
        val secureObject = Crypto.constructSecureObject(new BaseObject(id), from, to, ObjectType.post.id, obj.asInstanceOf[Post].toJson.compactPrint, keys)
        Crypto.constructSecureMessage(myBaseObj.id, secureObject.toJson.compactPrint, serverPublicKey, keyPair.getPrivate)
      case ObjectType.picture =>
        val secureObject = Crypto.constructSecureObject(new BaseObject(id), from, to, ObjectType.picture.id, obj.asInstanceOf[Picture].toJson.compactPrint, keys)
        Crypto.constructSecureMessage(myBaseObj.id, secureObject.toJson.compactPrint, serverPublicKey, keyPair.getPrivate)
      case ObjectType.album =>
        val secureObject = Crypto.constructSecureObject(new BaseObject(id), from, to, ObjectType.album.id, obj.asInstanceOf[Album].toJson.compactPrint, keys)
        Crypto.constructSecureMessage(myBaseObj.id, secureObject.toJson.compactPrint, serverPublicKey, keyPair.getPrivate)
    }
  }

  def createSecureRequestMessage(from: Int, to: Int, objType: ObjectType, objId: Int): SecureMessage = {
    val secureRequest = SecureRequest(from, to, objType.id, objId)
    Crypto.constructSecureMessage(myBaseObj.id, secureRequest.toJson.compactPrint, serverPublicKey, keyPair.getPrivate)
  }

  def decryptSecureObject(secureObject: SecureObject, objType: ObjectType):Any = {
    try {
      val aesKey = Crypto.constructAESKeyFromBytes(Crypto.decryptRSA(secureObject.encryptedKeys(myBaseObj.id.toString), keyPair.getPrivate))
      val objJson = Base64Util.decodeString(Crypto.decryptAES(secureObject.data, aesKey, Constants.IV))
      objType match {
        case ObjectType.user => JsonParser(objJson).convertTo[User]
        case ObjectType.page => JsonParser(objJson).convertTo[Page]
        case ObjectType.post => JsonParser(objJson).convertTo[Post]
        case ObjectType.picture => JsonParser(objJson).convertTo[Picture]
        case ObjectType.album => JsonParser(objJson).convertTo[Album]
      }
    }
    catch {
      case e: NoSuchElementException => log.info(s"${myBaseObj.id} doesn't have permission to decrypt ${secureObject.baseObj.id}")
      case d: BadPaddingException => log.info(s"${myBaseObj.id} can't decrypt ${secureObject.baseObj.id}")
    }
  }

  def decryptSecureRequestMessage(secureMsg: SecureMessage, objType: ObjectType): Any = {
    try {
      val requestKeyBytes = Crypto.decryptRSA(secureMsg.encryptedKey, keyPair.getPrivate)
      if (Crypto.verifySign(serverPublicKey, secureMsg.signature, requestKeyBytes)) {
        val objJson = Base64Util.decodeString(
          Crypto.decryptAES(secureMsg.message, Crypto.constructAESKeyFromBytes(requestKeyBytes), Constants.IV)
        )
        objType match {
          case ObjectType.intArray => JsonParser(objJson).convertTo[Array[Int]]
          case ObjectType.userKeyMap => JsonParser(objJson).convertTo[Map[String, Array[Byte]]]
          case ObjectType.secureObjectArray => JsonParser(objJson).convertTo[Array[SecureObject]]

        }
      }
    }
    catch{
      case e: BadPaddingException => log.info(s"${myBaseObj.id} can't decrypt SecureMessage")
    }
  }

  def decryptSecureObjectMessage(secureMsg: SecureMessage, objType: ObjectType): Any = {
    try {
      val requestKeyBytes = Crypto.decryptRSA(secureMsg.encryptedKey, keyPair.getPrivate)
      if (Crypto.verifySign(serverPublicKey, secureMsg.signature, requestKeyBytes)) {
        val json = Base64Util.decodeString(
          Crypto.decryptAES(secureMsg.message, Crypto.constructAESKeyFromBytes(requestKeyBytes), Constants.IV)
        )
        val secureObject = JsonParser(json).convertTo[SecureObject]
        try {
          val aesKey = Crypto.constructAESKeyFromBytes(Crypto.decryptRSA(secureObject.encryptedKeys(myBaseObj.id.toString), keyPair.getPrivate))
          val objJson = Base64Util.decodeString(Crypto.decryptAES(secureObject.data, aesKey, Constants.IV))
          objType match {
            case ObjectType.user => JsonParser(objJson).convertTo[User]
            case ObjectType.page => JsonParser(objJson).convertTo[Page]
            case ObjectType.post => JsonParser(objJson).convertTo[Post]
            case ObjectType.picture => JsonParser(objJson).convertTo[Picture]
            case ObjectType.album => JsonParser(objJson).convertTo[Album]
            case ObjectType.intArray => JsonParser(objJson).convertTo[Array[Int]]
            case ObjectType.userKeyMap => JsonParser(objJson).convertTo[Map[String, Array[Byte]]]
          }
        }
        catch {
          case e: NoSuchElementException => log.info(s"${myBaseObj.id} doesn't have permission to decrypt ${secureObject.baseObj.id}")
          case d: BadPaddingException => log.info(s"${myBaseObj.id} can't decrypt ${secureObject.baseObj.id}")
        }
      }
    }
    catch{
      case e: BadPaddingException => log.info(s"${myBaseObj.id} can't decrypt SecureMessage")
    }
  }
}