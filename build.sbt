name := "COP5615 Project 5"

version := "1.0"

scalaVersion := "2.11.6"

resolvers ++= Seq(
  "Spray repository" at "http://repo.spray.io",
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

scalacOptions := Seq("-deprecation")

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  Seq(
    "io.spray" %% "spray-can" % sprayV,
    "io.spray" %% "spray-routing" % sprayV,
    "io.spray" %% "spray-client" % sprayV,
    "io.spray" %% "spray-json" % "1.3.1",
    "io.spray" %% "spray-testkit" % sprayV % "test",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-testkit" % akkaV % "test",
    "joda-time" % "joda-time" % "2.9.1",
    "com.google.guava" % "guava" % "17.0",
    "org.scalatest" %"scalatest_2.11" % "2.2.1" % "test",
    "org.joda" % "joda-convert" % "1.2",
    "commons-codec" % "commons-codec" % "1.10",
    "org.scala-lang.modules" %% "scala-pickling" % "0.10.1"
  )
}