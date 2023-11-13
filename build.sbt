import sbt.*
import sbt.Keys.*

ThisBuild / version := "0.1.0"
ThisBuild / organization := "dev.celestica"

lazy val core = (project in file("core"))
  .settings(
    name := "erd-core",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scalameta" %% "scalameta" % "4.8.13",
      "com.lihaoyi" %% "fastparse" % "3.0.2",
      "org.antlr" % "antlr4-runtime" % "4.13.1",
      "org.scalatest" %% "scalatest" % "3.2.15" % Test
    ),
    Antlr4 / antlr4Version := "4.13.1",
    Antlr4 / antlr4PackageName := Some("dev.celestica.sql.parser.mysql"),
    Antlr4 / antlr4GenListener := true,
    Antlr4 / antlr4GenVisitor  := true
  ).enablePlugins(Antlr4Plugin)


lazy val erdPluginProject = (project in file("sbt-plugin"))
  .enablePlugins(SbtPlugin)
  .dependsOn(core)
  .settings(
    name := "sbt-erd-generator",
    sbtPlugin := true,
  )

lazy val root = (project in file("."))
  .aggregate(core, erdPluginProject)
  .settings(
    publish / skip := true
  )
