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

import com.goshoplane.sbt.standard.libraries.StandardLibraries

object CassieBuild extends Build with StandardLibraries {

  lazy val makeScript = TaskKey[Unit]("make-script", "make script in local directory to run main classes")

  def sharedSettings = Seq(
    organization := "com.goshoplane",
    version := "0.0.1",
    scalaVersion := Version.scala,
    crossScalaVersions := Seq(Version.scala, "2.11.4"),
    scalacOptions := Seq("-unchecked", "-optimize", "-deprecation", "-feature", "-language:higherKinds", "-language:implicitConversions", "-language:postfixOps", "-language:reflectiveCalls", "-Yinline-warnings", "-encoding", "utf8"),
    retrieveManaged := true,

    fork := true,
    javaOptions += "-Xmx2500M",

    resolvers ++= StandardResolvers,

    publishMavenStyle := true
  ) ++ net.virtualvoid.sbt.graph.Plugin.graphSettings

  lazy val cassie = Project(
    id = "cassie",
    base = file("."),
    settings = Project.defaultSettings
  ).aggregate(core, service, catalogue, store)


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
      ++ Libs.akka
      ++ Libs.scaldi
  )


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
      ++ Libs.commonsCatalogue
  ).dependsOn(core)


  lazy val store = Project(
    id = "cassie-store",
    base = file("store"),
    settings = Project.defaultSettings ++
      sharedSettings
      // SbtStartScript.startScriptForClassesSettings
  ).settings(
    name := "cassie-store",

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
      ++ Libs.commonsCatalogue
  ).dependsOn(core)


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
    ) ++ Libs.microservice,
    makeScript <<= (stage in Universal, stagingDirectory in Universal, baseDirectory in ThisBuild, streams) map { (_, dir, cwd, streams) =>
      var path = dir / "bin" / "cassie-service"
      sbt.Process(Seq("ln", "-sf", path.toString, "cassie-service"), cwd) ! streams.log
    }
  ).dependsOn(core, catalogue, store)


}