package Objects

import Utils.Crypto
import spray.json._
import spray.routing.RequestContext
import ObjectJsonSupport._
import scala.collection.mutable

case class BaseObject(var id: Int = -1, var deleted: Boolean = false) {
  val likes = mutable.Set[Int]()

  def updateId(newId: Int) = id = newId

  def appendLike(pid: Int) = likes.add(pid)

  def delete() = deleted = true
}