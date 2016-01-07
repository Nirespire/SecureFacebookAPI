package Objects.ObjectTypes

import spray.json._


object ObjectType extends Enumeration {
  type ObjectType = Value
  val user, page, post, picture, album, intArray, userKeyMap, feed, secureObjectArray = Value

  implicit object ObjectTypeJsonFormat extends RootJsonFormat[ObjectType.ObjectType] {
    def write(obj: ObjectType.ObjectType): JsValue = JsNumber(obj.id)

    def read(json: JsValue): ObjectType.ObjectType = json match {
      case JsNumber(id) => ObjectType(id.toInt)
      case _ => throw new DeserializationException("Enum int expected")
    }
  }

}
