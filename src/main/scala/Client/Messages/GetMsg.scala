package Client.Messages

import spray.http.HttpResponse

case class GetMsg(response: HttpResponse, reaction: String)