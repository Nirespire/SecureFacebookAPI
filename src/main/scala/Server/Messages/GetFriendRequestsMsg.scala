package Server.Messages

import spray.routing.RequestContext

case class GetFriendRequestsMsg(rc: RequestContext, pid: Int)