package Server.Messages

import spray.routing.RequestContext

case class GetFriendKeysMsg(rc: RequestContext, pid: Int)