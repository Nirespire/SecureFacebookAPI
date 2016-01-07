package Objects

import Objects.ObjectTypes.PostType.PostType

case class Post(
                 createdTime: String,
                 message: String,
                 postType: PostType,
                 attachmentId: Int
               )