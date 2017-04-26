name := """akka-netty"""

version := "1.0.2"

scalaVersion := "2.11.7"

val akkaVer = "2.4.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVer,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaVer,
  "io.netty" % "netty-all" % "4.1.2.Final",
  "ch.qos.logback" % "logback-classic" % "1.1.1",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)
