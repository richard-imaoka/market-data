// Using lazy val root =, since, from SBT 0.13, "Multi-project" .sbt definition is recommended instead of "Bare" .sbt definition.
// Multi-Project .sbt build definition = http://www.scala-sbt.org/0.13/tutorial/Basic-Def.html
// Bare .sbt definition = http://www.scala-sbt.org/0.13/tutorial/Bare-Def.html
lazy val root = (project in file(".")).settings(
    name := "market-data",
    version := "0.0",
    scalaVersion := "2.11.4",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.3.7" withSources() withJavadoc(),
      "com.typesafe.akka" %% "akka-testkit" % "2.3.7" % "test" withSources() withJavadoc(),
      "org.scalatest" %% "scalatest" % "2.2.1" % "test" withSources() withJavadoc()
    ),
    scalacOptions ++= Seq("-feature")
  )
