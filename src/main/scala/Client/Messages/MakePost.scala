package Client.Messages

import Objects.ObjectTypes.PostType.PostType


case class MakePost(postType:PostType, attachmentID:Int)
