package Server.Messages

import Objects.SecureObject
import spray.routing.RequestContext

case class PutSecureObjMsg(
                            rc: RequestContext,
                            secureObj: SecureObject
                          )