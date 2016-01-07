package Server.Messages

import Objects.SecureRequest
import spray.routing.RequestContext

case class LikeMsg(
                    rc: RequestContext,
                    secureRequest: SecureRequest
                  )