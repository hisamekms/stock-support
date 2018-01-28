name := """stock-support"""
organization := "xyz.hisamekms"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.3"

resolvers += Resolver.mavenLocal

libraryDependencies += guice
libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.3.0"
libraryDependencies += "com.h2database" % "h2" % "1.4.196"
libraryDependencies += ws
libraryDependencies += "com.typesafe.play" %% "play-slick" % "3.0.0"
libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "3.0.0"
// Adds additional packages into Twirl
//TwirlKeys.templateImports += "xyz.hisamekms.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "xyz.hisamekms.binders._"
libraryDependencies += "org.reactivemongo" %% "reactivemongo" % "0.12.7"
libraryDependencies += "org.reactivemongo" %% "reactivemongo-akkastream" % "0.12.7"
libraryDependencies += "com.jimmoores" % "quandl-core" % "2.0.0"
libraryDependencies += "javax.activation" % "activation" % "1.1.1"
libraryDependencies += "com.monitorjbl" % "xlsx-streamer" % "1.2.0"

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.8" % Test
libraryDependencies += "org.scalamock" %% "scalamock" % "4.0.0" % Test

javaOptions += "--illegal-access=warn"