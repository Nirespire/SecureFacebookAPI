package Server.Messages

import Objects.SecureRequest
import spray.routing.RequestContext

case class DeleteSecureObjMsg(
                               rc: RequestContext,
                               secureRequest: SecureRequest
                             )