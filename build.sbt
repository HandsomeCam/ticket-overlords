name := """ticket-overlords"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  specs2 % Test,
  cache,
  ws,
  filters,
  "org.webjars" % "jquery" % "2.1.4",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.2",
  "com.h2database" % "h2" % "1.4.192",
  "com.typesafe.play" %% "play-slick" % "2.0.2",
  "org.webjars" % "requirejs" % "2.1.22",
  "org.webjars" % "react" % "0.13.3"
)

