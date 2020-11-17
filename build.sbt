name := "stream-cancel-issue"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies ++= List(
 "org.http4s" %% "http4s-dsl" % "0.21.1",
 "org.http4s" %% "http4s-blaze-server" % "0.21.1",
 "org.http4s" %% "http4s-blaze-client" % "0.21.1",
 "org.http4s" %% "http4s-circe" % "0.21.1",
 "org.slf4j" % "slf4j-api" % "1.7.30",
 "ch.qos.logback" % "logback-classic" % "1.2.3"
)
