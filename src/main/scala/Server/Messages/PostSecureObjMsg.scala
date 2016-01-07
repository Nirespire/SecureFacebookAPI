package Server.Messages

import Objects.SecureObject
import spray.routing.RequestContext

case class PostSecureObjMsg(
                            rc: RequestContext,
                            secureObj: SecureObject
                          )