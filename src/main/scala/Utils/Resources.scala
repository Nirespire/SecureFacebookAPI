package Utils

import java.io.InputStream
import java.util.GregorianCalendar

import com.google.common.io.BaseEncoding

import scala.util.Random


object Resources {
  val statuses = Seq(
    "Laugh at your problems, everybody else does.",
    "Worrying works! 90% of the things I worry about never happen.",
    "I thought I wanted a career, turns out I just wanted paychecks.",
    "Nothing sucks more than that moment during an argument when you realize you’re wrong.",
    "Never get into fights with ugly people, they have nothing to lose.",
    "A little boy asked his father, “Daddy, how much does it cost to get married?” Father replied, “I don’t know son, I’m still paying.”",
    "Artificial intelligence is no match for natural stupidity.",
    "The longer the title the less important the job.",
    "Just remember…if the world didn’t suck, we’d all fall off.",
    "Never, under any circumstances, take a sleeping pill and a laxative on the same night.",
    "I didn’t say it was your fault, I said I was blaming you.",
    "The shinbone is a device for finding furniture in a dark room.",
    "Why does someone believe you when you say there are four billion stars, but check when you say the paint is wet?",
    "The sole purpose of a child’s middle name, is so he can tell when he’s really in trouble.",
    "Good girls are bad girls that never get caught.",
    "Some people say “If you can’t beat them, join them”. I say “If you can’t beat them, beat them”, because they will be expecting you to join them, so you will have the element of surprise.",
    "I’m never sure what to do with my eyes when I’m at the dentist. Do I close them? Do I stare at his face? Do I look at the ceiling? What’s the proper etiquette here?",
    "No, I’m not feeling violent, I’m feeling creative with weapons.",
    "You do not need a parachute to skydive. You only need a parachute to skydive twice.",
    "By the time a man realizes that his father was right, he has a son who thinks he’s wrong.",
    "Better to remain silent and be thought a fool, than to speak and remove all doubt.",
    "Some people are like Slinkies … not really good for anything, but you can’t help smiling when you see one tumble down the stairs.",
    "Did you know that dolphins are so smart that within a few weeks of captivity, they can train people to stand on the very edge of the pool and throw them fish?",
    "I totally take back all those times I didn’t want to nap when I was younger.",
    "A bank is a place that will lend you money, if you can prove that you don’t need it.",
    "Do not argue with an idiot. He will drag you down to his level and beat you with experience.",
    "If I agreed with you we’d both be wrong.",
    "Is it just me, or are 80% of the people in the “people you may know” feature on Facebook people that I do know, but I deliberately choose not to be friends with?",
    "The real reason women live longer than men because they don’t have to live with women.",
    "Eat right, exercise, die anyway.",
    "Knowledge is knowing a tomato is a fruit; Wisdom is not putting it in a fruit salad.",
    "Children: You spend the first 2 years of their life teaching them to walk and talk. Then you spend the next 16 years telling them to sit down and shut-up.",
    "Politicians and diapers have one thing in common. They should both be changed regularly, and for the same reason.",
    "Evening news is where they begin with ‘Good evening’, and then proceed to tell you why it isn’t.",
    "To steal ideas from one person is plagiarism. To steal from many is research.",
    "We buy things we don’t need, with money we don’t have, to impress people we don’t know.",
    "I may be fat, but you’re ugly – I can lose weight!",
    "I wish Google Maps had an “Avoid Ghetto” routing option.",
    "A husband is someone who after taking the trash out, gives the impression he just cleaned the whole house.",
    "Always borrow money from a pessimist. He won’t expect it back.",
    "A diplomat is someone who can tell you to go to hell in such a way that you will look forward to the trip.",
    "We have enough gun control. What we need is idiot control.",
    "My opinions may have changed, but not the fact that I am right.",
    "I intend to live forever. So far, so good.",
    "When in doubt, mumble.",
    "WARNING: The consumption of alcohol may make you think you are whispering when you are not.",
    "I like work. It fascinates me. I sit and look at it for hours.",
    "I used to be indecisive. Now I’m not sure.",
    "There is a great need for sarcasm font.",
    "Every so often, I like to go to the window, look up, and smile for a satellite picture.",
    "When tempted to fight fire with fire, remember that the Fire Department usually uses water.",
    "Worry is interest paid in advance for a debt you may never owe.",
    "The advantage of exercising every day is that you die healthier.",
    "Knowledge is power, and power corrupts. So study hard and be evil.",
    "I would rather try to carry 10 plastic grocery bags in each hand than take 2 trips to bring my groceries in.",
    "Some people hear voices.. Some see invisible people.. Others have no imagination whatsoever.",
    "If winning isn’t everything why do they keep score?",
    "After (M)onday and (T)uesday even the week says WTF !!",
    "Change is inevitable, except from a vending machine.",
    "Girls are like roads, more the curves, more the dangerous they are.",
    "Why didn’t Noah swat those two mosquitoes?",
    "The difference between in-laws and outlaws? Outlaws are wanted.",
    "Money talks…but all mine ever says is good-bye.",
    "Our generation doesn’t knock on doors. We will call or text to let you know we’re outside.",
    "They keep saying the right person will come along, I think mine got hit by a truck.",
    "If the number 2 pencil is the most popular, why is it still number 2?",
    "By the time you learn the rules of life, you’re too old to play the game.",
    "We are all time travelers moving at the speed of exactly 60 minutes per hour",
    "Dogs have masters. Cats have staff.",
    "I don’t have a beer gut, I have a protective covering for my rock hard abs.",
    "People tend to make rules for others and exceptions for themselves.",
    "I have all the money I’ll ever need – if I die by 4:00 p.m. today.",
    "Google Maps really needs to start their directions on #5. Pretty sure I know how to get out of my neighborhood.",
    "Life’s like a bird, it’s pretty cute until it craps on your head.",
    "Don’t steal. That’s the government’s job.",
    "A fine is a tax for doing wrong. A tax is a fine for doing well.",
    "Women should not have children after 35. Really… 35 children are enough.",
    "Lite: the new way to spell “Light,” now with 20% fewer letters!",
    "I went to see my doctor. “Doctor, every morning when I get up and look in the mirror, I feel like throwing up. What’s wrong with me?” He said “I don’t know but your eyesight is perfect.”",
    "There are no winners in life…only survivors.",
    "Some cause happiness wherever they go. Others whenever they go.",
    "Without ME, it’s just AWESO.",
    "The hardest thing to learn in life is which bridge to cross and which to burn.",
    "I’m in shape. Round is a shape isn’t it?",
    "The farther away the future is, the better it looks.",
    "There are two kinds of people who don’t say much: those who are quiet and those who talk a lot.",
    "I love to give homemade gifts. Which one of my kids do you want?",
    "We are all part of the ultimate statistic – ten out of ten die.",
    "I am willing to make the mistakes if someone else is willing to learn from them.",
    "If you do a job too well, you will get stuck with it.",
    "How do you get holy water? Boil the hell out of it.",
    "I’d like to help you out, which way did you come in?",
    "To find out a girl’s faults, praise her to her girlfriends.",
    "Asking dumb questions is easier than correcting dumb mistakes.",
    "Complex problems have simple, easy to understand, wrong answers.",
    "Seen it all, done it all, can’t remember most of it.",
    "The winner of the rat race is still a rat.",
    "If you think education is expensive, try ignorance.",
    "All of us could take a lesson from the weather. It pays no attention to criticism.",
    "Why do women always ask questions that have no right answers?",
    "I’m not a doctor but, I play one on TV.",
    "I love scotch. Scotchy, scotch, scotch. Here it goes down, down into my belly… (Or any Anchorman Quote)",
    "Facebook account for sale, Friends included.",
    "I drink to make other people interesting. – “George Jean Nathan”",
    "Using Shamwow to clean up my puke. Surprisingly works pretty well.",
    "can see Alaska from my house.",
    "So you’re telling me there’s a chance. (Or any Dumb&Dumber quote)",
    "Google just called… Google said, “Someone is looking for you”.",
    "Linking to this movie clip : http://www.youtube.com/watch?v=rYyD55elKJA",
    "I’m so stoked on my friends status updates. Going to the gym? Awesome! Pursuing your career in Babysitting? Rad! Going to sleep? Tell me more!!!"
  )

  def getRandomStatus(): String = {
    statuses(Random.nextInt(statuses.size))
  }

  val pageCategories = Seq(
    "Book or Magazine",
    "Brand or Product",
    "Company or Organization",
    "Local Business",
    "Movie",
    "Music",
    "Other",
    "People",
    "Sports",
    "Television",
    "Website or Blog"
  )

  def getRandomPageCategory(): String = {
    pageCategories(Random.nextInt(pageCategories.size))
  }


  val names = Seq(
    "Lavelle Bartley",
    "Kyra Villareal",
    "Marine Vigil",
    "Leonardo Tonn",
    "Larisa Neal",
    "Krystin Litchford",
    "Synthia Pitre",
    "Charlie Bode",
    "Ciera Bitner",
    "Jenette Bradsher",
    "Nettie Hoback",
    "Sal Bosket",
    "Darin Millay",
    "Miss Robey",
    "Cornelius Allison",
    "Calista Martyn",
    "Sterling Dorr",
    "Coral Glenn",
    "Lorette Omara",
    "Millard Maag",
    "Lynette Huss",
    "Zona Biffle",
    "Lucina Mccranie",
    "Marybelle Burney",
    "Melina Wallander",
    "Olene Alcon",
    "Audrey Sayles",
    "Kennith Ring",
    "Lena Schiller",
    "Glen Lacaze",
    "Britta Tillman",
    "Vena Strout",
    "Angelika Lukes",
    "Cathey Wallick",
    "Charlotte Neeson",
    "Mindi Seats",
    "Rochelle Putnam",
    "Violette Cahall",
    "Elena Cancel",
    "Walter Douglass",
    "Lynne Dimarco",
    "Tiffani Legere",
    "Genevive Souther",
    "Twana Krone",
    "Kenia Dalzell",
    "Willie Unruh",
    "Eldridge Glanton",
    "Tressie Gay",
    "Latoria Curiel",
    "Sarah Heinen",
    "Rickie Stauber",
    "Gino Beebe",
    "Dorathy Houde",
    "Marin Klem",
    "Brent Read",
    "Ronni Marsee",
    "Tommie Lasala",
    "Iliana Wortham",
    "Fallon Meyers",
    "Roxanna Scarborough",
    "Haydee Biehl",
    "Jeanie Neault",
    "Francesca Lesesne",
    "Karina Striplin",
    "Korey Bregman",
    "Bruce Peets",
    "Beata Vanegas",
    "Margene Marques",
    "Yukiko Hua",
    "Corey Kavanaugh",
    "Beverley Kuehne",
    "Lula Rega",
    "Van Tetzlaff",
    "Lesha Irwin",
    "Jacinto Claus",
    "Edris Yearta",
    "Tomika Hively",
    "Thelma Rozzell",
    "Trudy Likens",
    "Carrol Hensley",
    "Veronica Mabrey",
    "Hobert Gouveia",
    "Pamelia Hayman",
    "Daphne Darsey",
    "Ione Fox",
    "Lilliana Lafontaine",
    "Dwana Paschall",
    "Bethany Noyola",
    "Nancey Peacock",
    "Eldridge Archey",
    "Lyndsey Finlay",
    "Chadwick Cahall",
    "Gaynelle Weir",
    "Ken Millis",
    "Delila Geissler",
    "Mika Caprio",
    "Maritza Carnahan",
    "Syble Ismail",
    "Kellye Leitzel",
    "Lyla Alcocer"
  )

  def randomBirthday(): String ={

    val year:Int = randBetween(1900, 2010)
    val month:Int = randBetween(1,12)
    val day: Int = randBetween(1,27)

    return day + "-" +  month + "-" + year

  }

  def randBetween(start:Int, end:Int) :Int = {
    return start + Math.round(Math.random() * (end - start)).toInt
  }

  def getImageBytes(filename: String): String = {
    val is: InputStream = getClass.getResourceAsStream(filename)
    val bytes = Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte).toArray
    BaseEncoding.base64().encode(bytes)
  }


}
