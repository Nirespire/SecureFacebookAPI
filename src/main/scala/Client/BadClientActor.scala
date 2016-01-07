package Client

import java.security.PublicKey
import javax.crypto.BadPaddingException
import Client.Messages.{AcceptFriendRequests, Activity}
import Objects.ObjectTypes.ObjectType
import Objects.ObjectTypes.ObjectType._
import Objects.Post
import Objects._
import Utils.{Resources, Constants, Base64Util, Crypto}
import akka.actor.{ActorLogging, Actor}
import com.typesafe.config.ConfigFactory
import spray.client.pipelining
import spray.client.pipelining._
import spray.json.JsonParser
import spray.json._
import Objects.ObjectJsonSupport._
import scala.concurrent.duration._

import scala.util.{Try, Random, Failure, Success}

class BadClientActor() extends Actor with ActorLogging {

  import context.dispatcher

  var me: User = null
  var myBaseObj: BaseObject = null
  private val keyPair = Crypto.generateRSAKeys()
  var serverPublicKey: PublicKey = null
  val config = ConfigFactory.load()
  lazy val servicePort = Try(config.getInt("service.port")).getOrElse(8080)
  lazy val serviceHost = Try(config.getString("service.host")).getOrElse("localhost")

  def random(n: Int) = Random.nextInt(n)
  def durationSeconds(n: Int) = n.seconds
  def randomDuration(n: Int) = durationSeconds(random(n))

  def receive = {
    case true => registerMyself()
    case false => activity()
    case stolenId:Int =>
  }

  def activity() = {
//    log.info("hehehe bad stuff")
    context.system.scheduler.scheduleOnce(randomDuration(10), self, Constants.falseBool)
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
          case Failure(error) => log.error(error, s"Couldn't register")
        }
      case Failure(error) => log.error(error, s"Couldn't get server_key")
    }
  }


  def createSecureRequestMessage(from: Int, to: Int, objType: ObjectType, objId: Int): SecureMessage = {
    val secureRequest = SecureRequest(from, to, objType.id, objId)
    Crypto.constructSecureMessage(myBaseObj.id, secureRequest.toJson.compactPrint, serverPublicKey, keyPair.getPrivate)
  }

  def decryptSecureObject(secureObject: SecureObject, objType: ObjectType): Any = {
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
      case e: NoSuchElementException => e
      case d: BadPaddingException => d
    }
  }

  def decryptSecureRequestMessage(secureMsg: SecureMessage, objType: ObjectType): Any = {
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

  def decryptSecureObjectMessage(secureMsg: SecureMessage, objType: ObjectType): Any = {
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
        case e: NoSuchElementException => e
        case d: BadPaddingException => d
      }
    }
  }
}
