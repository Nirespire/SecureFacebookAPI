package Server.Actors

import Objects._
import Objects.ObjectTypes._
import ObjectJsonSupport._
import Server.Messages._
import Utils.{Constants, Crypto, DebugInfo}
import spray.json._
import spray.routing.RequestContext

class PageActor(var page: SecureObject, debugInfo: DebugInfo)
  extends ProfileActor(page.baseObj.id, debugInfo: DebugInfo) {

  def baseObject = page.baseObj

  def pageReceive: Receive = {
    case GetFeedMsg(rc, SecureRequest(from, _, _, _)) if baseObject.deleted => handlePageDeleted(rc, from)
    case LikeMsg(rc, SecureRequest(from, _, _, _)) if baseObject.deleted => handlePageDeleted(rc, from)
    case PostSecureObjMsg(rc, nPage@SecureObject(_, from, _, _, _, _)) if baseObject.deleted =>
      handlePageDeleted(rc, from)
    case DeleteSecureObjMsg(rc, SecureRequest(from, _, _, _)) if baseObject.deleted => handlePageDeleted(rc, from)
    case GetSecureObjMsg(rc, SecureRequest(from, _, _, _)) if baseObject.deleted => handlePageDeleted(rc, from)
    case DeleteSecureObjMsg(rc, SecureRequest(from, to, id, _)) if id == ObjectType.page.id =>
      if (from == to) {
        baseObject.delete()
        handlePageDeleted(rc, from)
      } else {
        handleUnauthorizedRequest(rc, from)
      }
    case GetSecureObjMsg(rc, SecureRequest(from, to, id, _)) if id == ObjectType.page.id => rc.complete(
      Crypto.constructSecureMessage(
        Constants.serverId,
        page.toJson.compactPrint,
        Constants.userPublicKeys(from),
        Constants.serverPrivateKey
      )
    )
    case PostSecureObjMsg(rc, nPage@SecureObject(_, from, to, id, _, _)) if id == ObjectType.page.id =>
      if (from == to) {
        page = nPage
        rc.complete(Crypto.constructSecureMessage(
          Constants.serverId,
          "Page Updated",
          Constants.userPublicKeys(from),
          Constants.serverPrivateKey
        ))
      } else {
        handleUnauthorizedRequest(rc, from)
      }
  }

  override def receive = pageReceive orElse super.receive


  def handlePageDeleted(rc: RequestContext, from: Int) = rc.complete(
    Crypto.constructSecureMessage(
      Constants.serverId,
      "Page Deleted!",
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
}