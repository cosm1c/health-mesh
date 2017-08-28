import sbt.Keys._

import scala.language.postfixOps
import scala.sys.process._

val akkaVersion = "2.5.4"
val akkaHttpVersion = "10.0.9"

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(

    organization := "prowse.github.cosm1c",

    name := "health-mesh",

    version := "1.0",

    scalaVersion := "2.12.3",

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "com.github.swagger-akka-http" %% "swagger-akka-http" % "0.10.0",

      "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
      "ch.qos.logback" % "logback-classic" % "1.2.3" % Runtime,
      "org.slf4j" % "jul-to-slf4j" % "1.7.25",

      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.0.1" % Test,
      "org.mockito" % "mockito-all" % "1.10.19" % Test
    ),

    scalacOptions ++= Seq(
      "-target:jvm-1.8",
      "-deprecation",
      "-encoding", "UTF-8", // yes, this is 2 args
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-unchecked",
      "-Xfatal-warnings",
      "-Xlint",
      "-Yno-adapted-args",
      //"-Ywarn-dead-code", // N.B. doesn't work well with the ??? hole
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Xfuture" //,
      //"-Ywarn-unused-import"     // 2.11 only, seems to cause issues with generated sources?
    ),

    javacOptions ++= Seq(
      "-source", "1.8",
      "-target", "1.8"
    ),

    // Ensures that static assets are packaged
    unmanagedResourceDirectories in Compile += ((baseDirectory in Compile) (_ / "target" / "classes")).value,

    // Build Info
    buildInfoOptions += BuildInfoOption.ToJson,
    buildInfoPackage := "prowse",
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      sbtVersion,
      BuildInfoKey.action("buildInstant") {
        java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now())
      },
      "gitChecksum" -> {
        git.gitHeadCommit
      })
  )

lazy val buildJs = taskKey[Unit]("Build JavaScript frontend")

buildJs := {
  println("Building JavaScript frontend...")
  Process("npm run ci-package", baseDirectory.value / "ui") !
}

assembly := (assembly dependsOn buildJs).value

packageBin in Compile := (packageBin in Compile dependsOn buildJs).value
