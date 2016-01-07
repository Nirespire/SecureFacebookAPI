package Server.Actors

import java.security.Key

import Objects.ObjectTypes.ObjectType
import Objects.SecureObject
import Server.Messages._
import Utils.{Constants, DebugInfo}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection.mutable

class DelegatorActor(serverPublicKey: Key, debugInfo: DebugInfo) extends Actor with ActorLogging {
  val profiles = mutable.HashMap[Int, ActorRef]()

  def receive = {
    case feedMsg@GetFeedMsg(rc, secureReq) =>
      debugInfo.debugVar(Constants.getFeedChar) += 1
      profiles(secureReq.to) ! feedMsg
    case PutSecureObjMsg(rc, secureObj: SecureObject) => ObjectType(secureObj.objectType) match {
      case ObjectType.user =>
        debugInfo.debugVar(Constants.putProfilesChar) += 1
        profiles.put(secureObj.to, context.actorOf(Props(new UserActor(secureObj, debugInfo))))
        rc.complete(secureObj.to.toString)
      case ObjectType.page =>
        debugInfo.debugVar(Constants.putProfilesChar) += 1
        profiles.put(secureObj.to, context.actorOf(Props(new PageActor(secureObj, debugInfo))))
        rc.complete(secureObj.to.toString)
      case _ =>
        ObjectType(secureObj.objectType) match {
          case ObjectType.post => debugInfo.debugVar(Constants.putPostsChar) += 1
          case ObjectType.picture => debugInfo.debugVar(Constants.putPicturesChar) += 1
          case ObjectType.album => debugInfo.debugVar(Constants.putAlbumsChar) += 1
        }
        profiles(secureObj.to) ! PutSecureObjMsg(rc, secureObj)
    }
    case postMsg@PostSecureObjMsg(rc, secureObj) =>
      ObjectType(secureObj.objectType) match {
        case ObjectType.user => debugInfo.debugVar(Constants.postUserChar) += 1
        case ObjectType.page => debugInfo.debugVar(Constants.postPageChar) += 1
        case ObjectType.post => debugInfo.debugVar(Constants.postPostChar) += 1
        case ObjectType.picture => debugInfo.debugVar(Constants.postPictureChar) += 1
        case ObjectType.album => debugInfo.debugVar(Constants.postAlbumChar) += 1
      }
      profiles(secureObj.to) ! postMsg
    case delMsg@DeleteSecureObjMsg(rc, secureReq) =>
      ObjectType(secureReq.objectType) match {
        case ObjectType.post => debugInfo.debugVar(Constants.deletePostChar) += 1
        case ObjectType.picture => debugInfo.debugVar(Constants.deletePictureChar) += 1
        case ObjectType.album => debugInfo.debugVar(Constants.deleteAlbumChar) += 1
      }
      profiles(secureReq.to) ! delMsg
    case getMsg@GetSecureObjMsg(rc, secureReq) =>
      ObjectType(secureReq.objectType) match {
        case ObjectType.post => debugInfo.debugVar(Constants.getPostsChar) += 1
        case ObjectType.picture => debugInfo.debugVar(Constants.getPicturesChar) += 1
        case ObjectType.album => debugInfo.debugVar(Constants.getAlbumsChar) += 1
        case ObjectType.user | ObjectType.page => debugInfo.debugVar(Constants.getProfilesChar) += 1
      }
      profiles(secureReq.to) ! getMsg
    case likeMsg@LikeMsg(rc, secureReq) =>
      debugInfo.debugVar(Constants.likeChar) += 2
      if (ObjectType(secureReq.objectType) == ObjectType.user) {
        profiles(secureReq.from) ! likeMsg
      } else {
        profiles(secureReq.to) ! likeMsg
      }
    case getFriendKeysMsg@GetFriendKeysMsg(rc, pid) =>
      debugInfo.debugVar(Constants.getFlChar) += 1
      profiles(pid) ! getFriendKeysMsg
    case friendReq@GetFriendRequestsMsg(rc, pid) =>
      debugInfo.debugVar(Constants.postFlChar) += 1
      profiles(pid) ! friendReq
    case addFriend@AddFriendMsg(rc, secureReq) =>
      debugInfo.debugVar(Constants.postFlChar) += 1
      profiles(secureReq.to) ! addFriend
    case x => log.error(s"Unhandled in DelegatorActor  $x")
  }
}