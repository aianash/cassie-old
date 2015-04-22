import sbt._
import sbt.Classpaths.publishTask
import Keys._

import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.{ MultiJvm, extraOptions, jvmOptions, scalatestOptions, multiNodeExecuteTests, multiNodeJavaName, multiNodeHostsFileName, multiNodeTargetDirName, multiTestOptions }

import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

import com.typesafe.sbt.SbtStartScript

import sbtassembly.AssemblyPlugin.autoImport._

import com.twitter.scrooge.ScroogeSBT

object CassieBuild extends Build with Libraries {
  def sharedSettings = Seq(
    organization := "com.goshoplane",
    version := "0.0.1",
    scalaVersion := Version.scala,
    crossScalaVersions := Seq(Version.scala, "2.11.4"),
    scalacOptions := Seq("-unchecked", "-optimize", "-deprecation", "-feature", "-language:higherKinds", "-language:implicitConversions", "-language:postfixOps", "-language:reflectiveCalls", "-Yinline-warnings", "-encoding", "utf8"),
    retrieveManaged := true,

    fork := true,
    javaOptions += "-Xmx2500M",

    resolvers ++= Seq(
      // "ReaderDeck Releases" at "http://repo.readerdeck.com/artifactory/readerdeck-releases",
      "anormcypher" at "http://repo.anormcypher.org/",
      "Akka Repository" at "http://repo.akka.io/releases",
      "Spray Repository" at "http://repo.spray.io/",
      "twitter-repo" at "http://maven.twttr.com",
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository"
    ),

    publishMavenStyle := true
  ) ++ net.virtualvoid.sbt.graph.Plugin.graphSettings

  lazy val cassie = Project(
    id = "cassie",
    base = file("."),
    settings = Project.defaultSettings
  ).aggregate(core, service, catalogue, store, asterix)


  lazy val core = Project(
    id = "cassie-core",
    base = file("core"),
    settings = Project.defaultSettings ++
      sharedSettings ++
      SbtStartScript.startScriptForClassesSettings
  ).settings(
    name := "cassie-core",

    libraryDependencies ++= Seq(
    ) ++ Libs.scalaz
      ++ Libs.scroogeCore
      ++ Libs.finagleThrift
      ++ Libs.libThrift
      ++ Libs.akka
      ++ Libs.scaldi
      ++ Libs.msgpack
  )


  lazy val store = Project(
    id = "cassie-store",
    base = file("store"),
    settings = Project.defaultSettings ++
      sharedSettings ++
      SbtStartScript.startScriptForClassesSettings
  ).settings(
    name := "cassie-store",

    libraryDependencies ++= Seq(
    ) ++ Libs.akka
      ++ Libs.slf4j
      ++ Libs.logback
      ++ Libs.phantom
      ++ Libs.playJson
      ++ Libs.catalogueCommons
  ).dependsOn(core)


  lazy val catalogue = Project(
    id = "cassie-catalogue",
    base = file("catalogue"),
    settings = Project.defaultSettings ++
      sharedSettings ++
      SbtStartScript.startScriptForClassesSettings
  ).settings(
    name := "cassie-catalogue",

    assemblyMergeStrategy in assembly := {
      case PathList(ps @ _*) if ps.last endsWith ".txt.1" => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },

    libraryDependencies ++= Seq(
    ) ++ Libs.akka
      ++ Libs.slf4j
      ++ Libs.logback
      ++ Libs.phantom
      ++ Libs.playJson
      ++ Libs.catalogueCommons
  ).dependsOn(core)


  lazy val asterix = Project(
    id = "cassie-asterix",
    base = file("asterix"),
    settings = Project.defaultSettings ++
      sharedSettings ++
      SbtStartScript.startScriptForClassesSettings
  ).settings(
    name := "cassie-asterix",

    assemblyMergeStrategy in assembly := {
      case PathList(ps @ _*) if ps.last endsWith ".txt.1" => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },

    libraryDependencies ++= Seq(
    ) ++ Libs.akka
      ++ Libs.slf4j
      ++ Libs.logback
      ++ Libs.finagleCore
      ++ Libs.mimepull
      ++ Libs.scaldi
      ++ Libs.scaldiAkka
      ++ Libs.bijection
      ++ Libs.kafka
      ++ Libs.msgpack
      ++ Libs.catalogueCommons
  ).dependsOn(core, catalogue, store)


  lazy val service = Project(
    id = "cassie-service",
    base = file("service"),
    settings = Project.defaultSettings ++
      sharedSettings ++
      SbtStartScript.startScriptForClassesSettings
  ).settings(
    name := "cassie-service",

    assemblyMergeStrategy in assembly := {
      case PathList(ps @ _*) if ps.last endsWith ".txt.1" => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },

    libraryDependencies ++= Seq(
    ) ++ Libs.akka
      ++ Libs.slf4j
      ++ Libs.logback
      ++ Libs.finagleCore
      ++ Libs.mimepull
      ++ Libs.scaldi
      ++ Libs.scaldiAkka
      ++ Libs.bijection
      ++ Libs.msgpack
  ).dependsOn(core, catalogue, store)


}