package Client.Messages

import Objects.ObjectTypes.PostType.PostType

case class UpdatePost(postType: PostType, attachmentID: Int)
