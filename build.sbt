
name := "downloadr"

version := "0.1"

scalaVersion := "2.10.1"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Classpaths.typesafeResolver

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)
