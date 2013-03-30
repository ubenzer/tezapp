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
    "org.apache.commons" % "commons-io" % "1.3.2",
    "org.apache.jena" % "jena-arq" % "2.9.3" excludeAll(ExclusionRule(organization = "org.slf4j")),
    "commons-validator" % "commons-validator" % "1.4.0",
    "org.apache.jena" % "jena-core" % "2.10.0" excludeAll(ExclusionRule(organization = "org.slf4j"))
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
