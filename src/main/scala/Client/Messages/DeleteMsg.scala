package Client.Messages

import spray.http.HttpResponse

case class DeleteMsg(response:HttpResponse, reaction:String)