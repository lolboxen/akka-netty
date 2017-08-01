name := """akka-netty"""

version := "1.0.2.5"

scalaVersion := "2.11.7"

val akkaVer = "2.4.14"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVer,
  "io.netty" % "netty-all" % "4.1.13.Final",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)
