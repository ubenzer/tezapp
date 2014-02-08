import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "tezapp"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    "org.openrdf.sesame" % "sesame-runtime" % "2.7.9",
    "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2"
  )

  // Only compile the bootstrap bootstrap.less file and any other *.less file in the stylesheets directory 
  def customLessEntryPoints(base: File): PathFinder =
      (base / "app" / "assets" / "css" / "bootstrap" * "bootstrap.less") +++
      (base / "app" / "assets" / "css" * "*.less")

  def customJSEntryPoints(base: File): PathFinder =
    base / "app" / "assets" / "js" ** "*.js"

  
  val main = play.Project(appName, appVersion, appDependencies).settings(
    lessEntryPoints <<= baseDirectory(customLessEntryPoints),
    javascriptEntryPoints <<= baseDirectory(customJSEntryPoints)
  )

  scalacOptions ++= Seq("-Xmx512M", "-Xmx2048M", "-XX:MaxPermSize=2048M")
}
