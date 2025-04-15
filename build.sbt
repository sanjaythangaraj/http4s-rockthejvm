ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.5"

lazy val root = (project in file("."))
  .settings(
    name := "http4s-rockthejvm"
  )

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-blaze-server" % "1.0.0-M32",
  "org.http4s" %% "http4s-dsl" % "1.0.0-M32",
  "org.http4s" %% "http4s-circe" % "1.0.0-M32",
  "io.circe" %% "circe-generic" % "0.14.12",
  "org.slf4j" % "slf4j-nop" % "2.0.17"
)
