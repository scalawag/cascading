import sbt._
import Keys._
import de.johoop.jacoco4sbt._
import JacocoPlugin._

object CascadingBuild extends Build {

  val project =
    Project("cascading",
            file("."),
            settings = Project.defaultSettings ++ Seq(
              name := "cascading",
              version := "0.1-SNAPSHOT",
              organization := "org.scalawag.cascading",
              crossPaths := false,
              scalacOptions ++= Seq("-unchecked","-deprecation","-feature"),
              javaOptions ++= Seq("-Xmx256m","-XX:MaxPermSize=256m"),
              scalaVersion := "2.10.0",
              testOptions += Tests.Argument("-oDF"),
              libraryDependencies ++= Seq(
                "org.slf4j" % "slf4j-api" % "1.6.1",
                "com.typesafe.akka" %% "akka-actor" % "2.1.0",
                "org.scalatest" %% "scalatest" % "1.9" % "test",
                "org.mockito" % "mockito-all" % "1.9.0" % "test",
                "org.scalawag.timber" % "slf4j-timber" % "0.2-SNAPSHOT" % "test"
              )
            ) ++ jacoco.settings )
}

/* cascading -- Copyright 2013 Justin Patterson -- All Rights Reserved */
