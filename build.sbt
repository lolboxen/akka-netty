name := """akka-netty"""

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.2",
  "io.netty" % "netty-all" % "4.1.0.CR2",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.2" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)


