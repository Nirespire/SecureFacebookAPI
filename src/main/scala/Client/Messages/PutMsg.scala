package Client.Messages

import spray.http.HttpResponse

case class PutMsg(response:HttpResponse, reaction:String)
