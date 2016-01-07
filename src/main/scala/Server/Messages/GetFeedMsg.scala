package Server.Messages

import Objects.SecureRequest
import spray.routing.RequestContext

case class GetFeedMsg(rc: RequestContext, secureRequest: SecureRequest)