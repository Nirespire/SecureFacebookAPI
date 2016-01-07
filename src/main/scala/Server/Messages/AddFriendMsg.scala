package Server.Messages

import Objects.SecureRequest
import spray.routing.RequestContext

case class AddFriendMsg(rc: RequestContext, secureRequest: SecureRequest)