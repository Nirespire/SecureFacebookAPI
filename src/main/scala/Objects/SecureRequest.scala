package Objects

case class SecureRequest(
                          from: Int,
                          to: Int,
                          objectType: Int,
                          getIdx: Int
                        )