package Client.Messages

import spray.http.HttpResponse

case class PostMsg(response:HttpResponse, reaction:String)
