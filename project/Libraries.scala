import sbt._
import Keys._

/**
 * This maintains common libraries and their versions
 * to be used like a standard. But one can change
 * it according to convenience.
 */
trait Libraries {
  object Version {
    val scala           = "2.10.4"
    val lucene          = "4.8.0"
    val akka            = "2.3.6"
    val scalaz          = "7.0.6"
    val graphstream     = "1.2"
    val fastutil        = "6.5.15"
    val argonaut        = "6.0.4"
    val neo4j           = "2.0.3"
    val stanfordCoreNlp = "3.3.1"
    val factorie        = "1.0"
    val twitterUtil     = "6.22.2"
    val finagle         = "6.22.0"
    val slf4j           = "1.7.6"
    val scallop         = "0.9.4"
    val stringmetric    = "0.25.3"
    val mimepull        = "1.9.3"
    val commonsLang     = "2.6"
    val provoz          = "0.0.1"
    val hemingway       = "1.0.0"
    val libThrift       = "0.8.0" // [check update to] 0.9.1
    val bijection       = "0.6.2"
    val scrooge         = "3.17.0"
    val commonsConfig   = "1.9"
    val guava           = "15.0"
    val wikiparser      = "1.0.0"
    val scalaGraph      = "1.8.0"
    val msgpack         = "0.6.8"
    val jsoup           = "1.7.3"
    val commonsCompress = "1.6"
    val clearnlp        = "2.0.2"
    val logback         = "1.1.2"
    val scaldi          = "0.3.2"
    val storehaus       = "0.9.0"
    val retry           = "0.2.0"
    val odelay          = "0.1.0"
    val researchpaperParser = "1.0"
    val kafka           = "0.8.2.0"
  }



  object Libs {

    val factorie = Seq (
      "cc.factorie" % "factorie" % Version.factorie)


    val odelayCore = Seq (
      "me.lessis" %% "odelay-core" % Version.odelay)


    val retry = Seq (
      "me.lessis" %% "retry" % Version.retry)


    val storehausCache = Seq (
      "com.twitter" %% "storehaus-cache" % Version.storehaus)


    val scaldi = Seq (
      "org.scaldi" %% "scaldi" % Version.scaldi)


    val scaldiAkka = Seq (
      "org.scaldi" %% "scaldi-akka" % Version.scaldi)


    val logback = Seq (
      "ch.qos.logback" % "logback-core" % Version.logback,
      "ch.qos.logback" % "logback-classic" % Version.logback)


    val researchpaperParser = Seq (
      "in.codehub" % "research-paper-parser" % Version.researchpaperParser)


    val commonsCompress = Seq (
      "org.apache.commons" % "commons-compress" % Version.commonsCompress)


    val jsoup = Seq (
      "org.jsoup" % "jsoup" % Version.jsoup)


    val msgpack = Seq (
      "org.msgpack" %% "msgpack-scala" % Version.msgpack)


    val scalaGraph = Seq (
      "com.assembla.scala-incubator" %% "graph-core" % Version.scalaGraph)


    val wikiparser = Seq (
      "com.readerdeck" %% "wikia-parser" % Version.wikiparser)


    val guava = Seq (
      "com.google.guava" % "guava" % Version.guava)


    val lucene = Seq (
      "org.apache.lucene" % "lucene-core"             % Version.lucene,
      "org.apache.lucene" % "lucene-analyzers-common" % Version.lucene,
      "org.apache.lucene" % "lucene-queries"          % Version.lucene,
      "org.apache.lucene" % "lucene-queryparser"      % Version.lucene
    )


    val akka = Seq (
      "com.typesafe.akka" %% "akka-slf4j" % Version.akka,
      "com.typesafe.akka" %% "akka-actor" % Version.akka)


    val akkaCluster = Seq (
      "com.typesafe.akka" %% "akka-cluster" % Version.akka)

    val akkaContrib = Seq (
      "com.typesafe.akka" %% "akka-contrib" % Version.akka)

    val slf4j = Seq (
      "org.slf4j" % "slf4j-api"     % Version.slf4j)


    val clearnlp = Seq (
      "com.clearnlp" % "clearnlp"                 % Version.clearnlp,
      "com.clearnlp" % "clearnlp-dictionary"      % "1.0",
      "com.clearnlp" % "clearnlp-general-en-dep"  % "1.2",
      "com.clearnlp" % "clearnlp-general-en-pos"  % "1.1",
      "com.clearnlp" % "clearnlp-general-en-srl"  % "1.1")


    val scalaz = Seq (
      "org.scalaz" %% "scalaz-core" % Version.scalaz)


    val graphstream = Seq (
      "org.graphstream" % "gs-core" % Version.graphstream,
      "org.graphstream" % "gs-algo" % Version.graphstream,
      "org.graphstream" % "gs-ui"   % Version.graphstream)


    val fastutil = Seq (
      "it.unimi.dsi" % "fastutil" % Version.fastutil)


    val argonaut = Seq (
      "io.argonaut" %% "argonaut" % Version.argonaut)


    val neo4j = Seq (
      "org.neo4j" % "neo4j-kernel"          % Version.neo4j,
      "org.neo4j" % "neo4j-graph-algo"      % Version.neo4j,
      "org.neo4j" % "neo4j-graph-matching"  % Version.neo4j)


    val stanfordCoreNlp = Seq (
      "edu.stanford.nlp" % "stanford-corenlp" % Version.stanfordCoreNlp artifacts (Artifact("stanford-corenlp", "models"), Artifact("stanford-corenlp")))


    val finagleCore = Seq (
      "com.twitter" %% "finagle-core"   % Version.finagle)


    val finagleThrift = Seq (
      "com.twitter" %% "finagle-thrift" % Version.finagle)


    val scallop = Seq (
      "org.rogach"  %% "scallop" % Version.scallop)


    val stringmetric = Seq (
      "com.rockymadden.stringmetric" % "stringmetric-core" % Version.stringmetric)


    val mimepull = Seq (
      "org.jvnet.mimepull" % "mimepull" % Version.mimepull)


    val commonsLang = Seq (
      "commons-lang" % "commons-lang" % Version.commonsLang)


    val scalaJLine = Seq (
      "org.scala-lang" % "jline" % Version.scala)


    val provozCore = Seq (
      "com.readerdeck" %% "provoz-core"  % Version.provoz)


    val hemingwayCore = Seq (
      "com.readerdeck" %% "hemingway-core" % Version.hemingway)


    val libThrift = Seq (
      "org.apache.thrift" % "libthrift" % Version.libThrift intransitive)


    val bijection = Seq (
      "com.twitter" %% "bijection-core" % Version.bijection,
      "com.twitter" %% "bijection-util" % Version.bijection)


    val scroogeCore = Seq (
      "com.twitter" %% "scrooge-core"   % Version.scrooge)


    val commonsConfig = Seq (
      "commons-configuration" % "commons-configuration" % Version.commonsConfig)


    val twitterUtil = Seq (
      "com.twitter" %% "util-core"      % Version.twitterUtil)

    val kafka = Seq (
      "org.apache.kafka" % "kafka-clients" % Version.kafka,
      "org.apache.kafka" %% "kafka" % Version.kafka )

  }
}