import sbt._
import sbt.Classpaths.publishTask
import Keys._

import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.{ MultiJvm, extraOptions, jvmOptions, scalatestOptions, multiNodeExecuteTests, multiNodeJavaName, multiNodeHostsFileName, multiNodeTargetDirName, multiTestOptions }
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging

import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

// import com.typesafe.sbt.SbtStartScript

import sbtassembly.AssemblyPlugin.autoImport._

import com.twitter.scrooge.ScroogeSBT

import com.typesafe.sbt.SbtNativePackager._, autoImport._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd, CmdLike}

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
      "websudos-repo" at "http://maven.websudos.co.uk/ext-release-local",
      Resolver.bintrayRepo("websudos", "oss-releases"),
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
      sharedSettings
      // SbtStartScript.startScriptForClassesSettings
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
      sharedSettings
      // SbtStartScript.startScriptForClassesSettings
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
      sharedSettings
      // SbtStartScript.startScriptForClassesSettings
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
      sharedSettings
      // SbtStartScript.startScriptForClassesSettings
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
      sharedSettings
      // SbtStartScript.startScriptForClassesSettings
  ).enablePlugins(JavaAppPackaging)
  .settings(
    name := "cassie-service",
    mainClass in Compile := Some("cassie.service.CassieServer"),

    dockerExposedPorts := Seq(4848),
    // TODO: remove echo statement once verified
    dockerEntrypoint := Seq("sh", "-c", "export CASSIE_HOST=`/sbin/ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1 }'` && echo $CASSIE_HOST && bin/cassie-service $*"),
    dockerRepository := Some("docker"),
    dockerBaseImage := "phusion/baseimage",
    dockerCommands ++= Seq(
      Cmd("USER", "root"),
      new CmdLike {
        def makeContent = """|RUN \
                             |  echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
                             |  add-apt-repository -y ppa:webupd8team/java && \
                             |  apt-get update && \
                             |  apt-get install -y oracle-java7-installer && \
                             |  rm -rf /var/lib/apt/lists/* && \
                             |  rm -rf /var/cache/oracle-jdk7-installer""".stripMargin
      }
    ),
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