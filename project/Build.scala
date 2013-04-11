import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "tezapp"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    javaCore,
    javaJdbc,
    javaEbean,
    "commons-io" % "commons-io" % "2.4",
    "commons-validator" % "commons-validator" % "1.4.0",
    "xerces" % "xercesImpl" % "2.11.0",
    "org.apache.jena" % "jena-core" % "2.10.0" excludeAll(ExclusionRule(organization = "org.slf4j"), ExclusionRule(organization = "xerces"))
  )

  // Only compile the bootstrap bootstrap.less file and any other *.less file in the stylesheets directory 
  def customLessEntryPoints(base: File): PathFinder = ( 
      (base / "app" / "assets" / "css" / "bootstrap" * "bootstrap.less") +++
      (base / "app" / "assets" / "css" / "bootstrap" * "responsive.less") +++ 
      (base / "app" / "assets" / "css" * "*.less")
  )
  
  val main = play.Project(appName, appVersion, appDependencies).settings(
    lessEntryPoints <<= baseDirectory(customLessEntryPoints)
  )
  
}
