ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.5"

lazy val root = (project in file("."))
  .settings(
    name := "http4s-rockthejvm"
  )

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-ember-server" % "1.0.0-M44",
  "org.http4s" %% "http4s-dsl" % "1.0.0-M44",
  "org.http4s" %% "http4s-circe" % "1.0.0-M44",
  "io.circe" %% "circe-generic" % "0.14.12",
  "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
  "org.slf4j" % "slf4j-simple" % "2.0.17"
)
