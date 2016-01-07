package Client.Messages

import spray.http.HttpResponse

case class DebugGetMsg(response: HttpResponse, reaction: String, url: String)
