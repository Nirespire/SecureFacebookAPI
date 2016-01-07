package Server.Actors

import Objects.ObjectTypes.ObjectType
import Objects._
import Objects.ObjectJsonSupport._
import Server.Messages._
import Utils.{Constants, Crypto, DebugInfo}
import spray.json._
import spray.routing.RequestContext

import scala.collection.mutable

class UserActor(var user: SecureObject, debugInfo: DebugInfo)
  extends ProfileActor(user.baseObj.id, debugInfo: DebugInfo) {

  val pendingRequests = mutable.Set[Int]()

  def baseObject = user.baseObj

  def userReceive: Receive = {
    case GetFeedMsg(rc, SecureRequest(from, _, _, _)) if baseObject.deleted => handleUserDeleted(rc, from)
    case LikeMsg(rc, SecureRequest(from, _, _, _)) if baseObject.deleted => handleUserDeleted(rc, from)
    case PostSecureObjMsg(rc, SecureObject(_, from, _, _, _, _)) if baseObject.deleted => handleUserDeleted(rc, from)
    case DeleteSecureObjMsg(rc, SecureRequest(from, _, _, _)) if baseObject.deleted => handleUserDeleted(rc, from)
    case GetSecureObjMsg(rc, SecureRequest(from, _, _, _)) if baseObject.deleted => handleUserDeleted(rc, from)
    case GetFriendRequestsMsg(rc, _) if baseObject.deleted => handleUserDeleted(rc, pid)
    case AddFriendMsg(rc, _) if baseObject.deleted => handleUserDeleted(rc, pid)
    case GetFriendKeysMsg(rc, _) if baseObject.deleted => handleUserDeleted(rc, pid)

    case GetFeedMsg(rc, SecureRequest(from, _, _, _)) if from != pid && !baseObject.likes.contains(from) =>
      handleUnauthorizedFeedRequest(rc, from)
    case PostSecureObjMsg(rc, SecureObject(_, from, _, _, _, _)) if from != pid => handleUnauthorizedRequest(rc, from)
    case DeleteSecureObjMsg(rc, SecureRequest(from, _, _, _)) if from != pid => handleUnauthorizedRequest(rc, from)
    case GetSecureObjMsg(rc, SecureRequest(from, _, _, _)) if from != pid && !baseObject.likes.contains(from) =>
      handleUnauthorizedRequest(rc, from)
    case GetFriendRequestsMsg(rc, from) if from != pid => handleUnauthorizedRequest(rc, from)
    case GetFriendKeysMsg(rc, from) if from != pid => handleUnauthorizedRequest(rc, from)

    case GetFriendRequestsMsg(rc, _) => rc.complete(
      Crypto.constructSecureMessage(
        Constants.serverId,
        pendingRequests.toArray.toJson.compactPrint,
        Constants.userPublicKeys(pid),
        Constants.serverPrivateKey
      )
    )
    case AddFriendMsg(rc, secureReq) =>
      pendingRequests.add(secureReq.from)
      rc.complete(s"${secureReq.from} Requested $pid to be friend")
    case LikeMsg(rc, secureReq) =>
      ObjectType(secureReq.objectType) match {
        case ObjectType.user =>

          baseObject.appendLike(secureReq.to)
          if (pendingRequests.contains(secureReq.to)) {
            pendingRequests.remove(secureReq.to)
            rc.complete("Added Friend!")
          }
      }
    case GetFriendKeysMsg(rc, _) =>
      val friendsKeyMap = baseObject.likes.map { fid =>
        (fid.toString, Constants.userPublicKeys(fid).getEncoded)
      }.toMap

      rc.complete(
        Crypto.constructSecureMessage(
          Constants.serverId,
          friendsKeyMap.toJson.compactPrint,
          Constants.userPublicKeys(pid),
          Constants.serverPrivateKey
        )
      )
    case DeleteSecureObjMsg(rc, SecureRequest(from, to, oid, _)) if oid == ObjectType.user.id =>
      if (from == to) {
        baseObject.delete()
        handleUserDeleted(rc, from)
      } else {
        handleUnauthorizedRequest(rc, from)
      }
    case GetSecureObjMsg(rc, SecureRequest(from, to, oid, _)) if oid == ObjectType.user.id =>
      rc.complete(
        Crypto.constructSecureMessage(
          Constants.serverId,
          user.toJson.compactPrint,
          Constants.userPublicKeys(from),
          Constants.serverPrivateKey
        )
      )
    case PostSecureObjMsg(rc, nUser@SecureObject(_, from, to, oid, _, _)) if oid == ObjectType.user.id =>
      if (from == to) {
        user = nUser
        rc.complete(Crypto.constructSecureMessage(
          Constants.serverId,
          "User Updated",
          Constants.userPublicKeys(from),
          Constants.serverPrivateKey
        ))
      } else {
        handleUnauthorizedRequest(rc, from)
      }
  }

  override def receive = userReceive orElse super.receive

  def handleUserDeleted(rc: RequestContext, from: Int) = rc.complete(
    Crypto.constructSecureMessage(
      Constants.serverId,
      "User Deleted!",
      Constants.userPublicKeys(from),
      Constants.serverPrivateKey
    )
  )

  def handleUnauthorizedRequest(rc: RequestContext, from: Int) = rc.complete(
    Crypto.constructSecureMessage(
      Constants.serverId,
      "Unauthorized Request! Not Request!",
      Constants.userPublicKeys(from),
      Constants.serverPrivateKey
    )
  )

  def handleUnauthorizedFeedRequest(rc: RequestContext, from: Int) = rc.complete(
    Crypto.constructSecureMessage(
      Constants.serverId,
      Array[SecureObject]().toJson.compactPrint,
      Constants.userPublicKeys(from),
      Constants.serverPrivateKey
    )
  )
}
