package Objects.ObjectTypes

import spray.json._

object PostType extends Enumeration {
  type PostType = Value
  val empty,link, status, photo = Value

  implicit object PostTypeJsonFormat extends RootJsonFormat[PostType.PostType] {
    def write(obj: PostType.PostType): JsValue = JsNumber(obj.id)

    def read(json: JsValue): PostType.PostType = json match {
      case JsNumber(id) => PostType(id.toInt)
      case _ => throw new DeserializationException("Enum string expected")
    }
  }

}



