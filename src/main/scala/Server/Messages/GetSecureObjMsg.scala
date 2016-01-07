package Server.Messages

import Objects.SecureRequest
import spray.routing.RequestContext

case class GetSecureObjMsg(rc: RequestContext, secureRequest: SecureRequest)