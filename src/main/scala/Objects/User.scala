package Objects

case class User(
                 about: String,
                 birthday: String,
                 gender: Char,
                 first_name: String,
                 last_name: String,
                 publicKey: Array[Byte]
               )