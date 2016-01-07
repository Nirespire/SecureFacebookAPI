package Objects

import Utils.{DebugInfo, Constants}
import spray.httpx.SprayJsonSupport
import spray.json._

object ObjectJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  def setToJsArray(setObj: scala.collection.mutable.Set[Int]) = JsArray(setObj.map(JsNumber(_)).toVector)


  implicit object BaseObjectJsonFormat extends RootJsonFormat[BaseObject] {
    def write(bs: BaseObject) = JsObject("id" -> JsNumber(bs.id),
      "likes" -> setToJsArray(bs.likes))

    def read(json: JsValue) = json.asJsObject.getFields("id", "likes") match {
      case Seq(JsNumber(id), JsArray(ids)) =>
        val bs = BaseObject(id.toInt)
        ids.foreach { likeId => bs.appendLike(likeId.convertTo[Int]) }
        bs
      case _ => throw new DeserializationException("Failed to deser BaseObject")
    }
  }

  implicit object DebugActorJsonFormat extends RootJsonFormat[DebugInfo] {
    def write(da: DebugInfo) = JsObject(
      "put-profiles" -> JsNumber(da.debugVar(Constants.putProfilesChar)),
      "put-posts" -> JsNumber(da.debugVar(Constants.putPostsChar)),
      "put-albums" -> JsNumber(da.debugVar(Constants.putAlbumsChar)),
      "put-pictures" -> JsNumber(da.debugVar(Constants.putPicturesChar)),
      "put-requestPersecond" -> JsNumber(da.putRequestPerSecond()),
      "post-friendlistUpdates" -> JsNumber(da.debugVar(Constants.postFlChar)),
      "post-userUpdates" -> JsNumber(da.debugVar(Constants.postUserChar)),
      "post-pageUpdates" -> JsNumber(da.debugVar(Constants.postPageChar)),
      "post-pictureUpdates" -> JsNumber(da.debugVar(Constants.postPictureChar)),
      "post-postUpdates" -> JsNumber(da.debugVar(Constants.postPostChar)),
      "post-albumUpdates" -> JsNumber(da.debugVar(Constants.postAlbumChar)),
      "post-requestPersecond" -> JsNumber(da.postRequestPerSecond()),
      "delete-users" -> JsNumber(da.debugVar(Constants.deleteUserChar)),
      "delete-pages" -> JsNumber(da.debugVar(Constants.deletePageChar)),
      "delete-posts" -> JsNumber(da.debugVar(Constants.deletePostChar)),
      "delete-pictures" -> JsNumber(da.debugVar(Constants.deletePictureChar)),
      "delete-albums" -> JsNumber(da.debugVar(Constants.deleteAlbumChar)),
      "delete-requestPersecond" -> JsNumber(da.deleteRequestPerSecond()),
      "get-profiles" -> JsNumber(da.debugVar(Constants.getProfilesChar)),
      "get-posts" -> JsNumber(da.debugVar(Constants.getPostsChar)),
      "get-albums" -> JsNumber(da.debugVar(Constants.getAlbumsChar)),
      "get-pictures" -> JsNumber(da.debugVar(Constants.getPicturesChar)),
      "get-friendlistUpdates" -> JsNumber(da.debugVar(Constants.getFlChar)),
      "get-requestPersecond" -> JsNumber(da.getRequestPerSecond()),
      "get-feed" -> JsNumber(da.debugVar(Constants.getFeedChar)),
      "likes" -> JsNumber(da.debugVar(Constants.postLikeChar)),
      "all-requestPersecond" -> JsNumber(da.allRequestPerSecond())
    )
    def read(value: JsValue) = {
      val da = DebugInfo()
      value.asJsObject.getFields("put-profiles", "put-posts", "put-albums", "put-pictures",
        "post-friendlistUpdates", "post-userUpdates", "post-pageUpdates", "post-pictureUpdates", "post-postUpdates", "post-albumUpdates",
        "delete-users", "delete-pages", "delete-posts", "delete-pictures", "delete-albums",
        "get-profiles", "get-posts", "get-albums", "get-pictures", "get-friendlistUpdates", "get-feed", "likes") match {
        case Seq(JsNumber(put_profiles),
        JsNumber(put_posts),
        JsNumber(put_albums),
        JsNumber(put_pictures),
        JsNumber(post_friendlistUpdates),
        JsNumber(post_userUpdates),
        JsNumber(post_pageUpdates),
        JsNumber(post_postUpdates),
        JsNumber(post_pictureUpdates),
        JsNumber(post_albumUpdates),
        JsNumber(delete_users),
        JsNumber(delete_pages),
        JsNumber(delete_posts),
        JsNumber(delete_pictures),
        JsNumber(delete_albums),
        JsNumber(get_profiles),
        JsNumber(get_posts),
        JsNumber(get_albums),
        JsNumber(get_pictures),
        JsNumber(get_friendlistUpdates),
        JsNumber(get_feed),
        JsNumber(likes)) =>
          da.debugVar(Constants.putProfilesChar) = put_profiles.toInt
          da.debugVar(Constants.putPostsChar) = put_posts.toInt
          da.debugVar(Constants.putAlbumsChar) = put_albums.toInt
          da.debugVar(Constants.putPicturesChar) = put_pictures.toInt
          da.debugVar(Constants.postFlChar) = post_friendlistUpdates.toInt
          da.debugVar(Constants.postUserChar) = post_userUpdates.toInt
          da.debugVar(Constants.postPageChar) = post_pageUpdates.toInt
          da.debugVar(Constants.postPostChar) = post_postUpdates.toInt
          da.debugVar(Constants.postPictureChar) = post_pictureUpdates.toInt
          da.debugVar(Constants.postAlbumChar) = post_albumUpdates.toInt
          da.debugVar(Constants.deleteUserChar) = delete_users.toInt
          da.debugVar(Constants.deletePageChar) = delete_pages.toInt
          da.debugVar(Constants.deletePostChar) = delete_posts.toInt
          da.debugVar(Constants.deletePictureChar) = delete_pictures.toInt
          da.debugVar(Constants.deleteAlbumChar) = delete_albums.toInt
          da.debugVar(Constants.getProfilesChar) = get_profiles.toInt
          da.debugVar(Constants.getPostsChar) = get_posts.toInt
          da.debugVar(Constants.getAlbumsChar) = get_albums.toInt
          da.debugVar(Constants.getPicturesChar) = get_pictures.toInt
          da.debugVar(Constants.getFlChar) = get_friendlistUpdates.toInt
          da.debugVar(Constants.getFeedChar) = get_feed.toInt
          da.debugVar(Constants.postLikeChar) = likes.toInt
          da
        case _ => throw new DeserializationException("Debug Actor expected")
      }
    }
  }

  implicit object AlbumJsonFormat extends RootJsonFormat[Album] {
    def write(a: Album) = JsObject(
      "createdTime" -> JsString(a.createdTime),
      "updatedTime" -> JsString(a.updatedTime),
      "coverPhoto" -> JsNumber(a.coverPhoto),
      "description" -> JsString(a.description),
      "pictures" -> setToJsArray(a.pictures)
    )

    def read(json: JsValue) = json.asJsObject.
      getFields("createdTime", "updatedTime", "coverPhoto", "description", "pictures") match {
      case Seq(JsString(cTime), JsString(uTime), JsNumber(cInt), JsString(desc), JsArray(pics)) =>
        val a = Album(cTime, uTime, cInt.toInt, desc)
        pics.foreach { pic => a.pictures.add(pic.convertTo[Int]) }
        a
      case _ => throw new DeserializationException("Failed to deser Album")
    }
  }

  implicit val PostJsonFormat = jsonFormat4(Post)
  implicit val FriendListJsonFormat = jsonFormat2(FriendList)
  implicit val PageJsonFormat = jsonFormat4(Page)
  implicit val PictureJsonFormat = jsonFormat2(Picture)
  implicit val UserJsonFormat = jsonFormat6(User)
  implicit val SecureObjectJsonFormat = jsonFormat6(SecureObject)
  implicit val SecureServerRequestJsonFormat = jsonFormat4(SecureMessage)
  implicit val SecureRequestJsonFormat = jsonFormat4(SecureRequest)
}

