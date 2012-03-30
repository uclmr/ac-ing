name := "optiassign"

organization := "org.riedelcastro.optiassign"

// The := method used in Name and Version is one of two fundamental methods.
// The other method is <<=
// All other initialization methods are implemented in terms of these.
version := "0.1-SNAPSHOT"

scalaVersion := "2.9.1"

resolvers += "conjars.org" at "http://conjars.org/repo"

scalacOptions += "-unchecked"

resolvers ++= Seq(
    "IESL third party" at "https://dev-iesl.cs.umass.edu:8081/nexus/content/repositories/thirdparty/",
    "IESL snapshots" at "https://dev-iesl.cs.umass.edu/nexus/content/repositories/snapshots",
    "IESL releases" at "https://dev-iesl.cs.umass.edu:8081/nexus/content/repositories/releases"
)

javaOptions in run += "-Xmx5G"

// Add multiple dependencies
libraryDependencies ++= Seq(
    "thirdparty" % "jgrapht-jdk1.6" % "0.8.2",
     "junit" % "junit" % "4.8" % "test",
     "log4j" % "log4j" % "1.2.16",
     "trove" % "trove" % "1.0.2",
     //"cc.refectorie.user.sameer" % "util" % "1.1-SNAPSHOT",
     "org.riedelcastro.nurupo" %% "nurupo" % "0.1-SNAPSHOT")
