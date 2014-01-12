import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  resolvers += Resolver.sonatypeRepo("snapshots")

  val appName         = "tezapp"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    "org.mongodb" %% "casbah" % "2.6.3",
    "org.openrdf.sesame" % "sesame-runtime" % "2.7.5",
    "commons-io" % "commons-io" % "2.4",
    "commons-validator" % "commons-validator" % "1.4.0"
  )

  // Only compile the bootstrap bootstrap.less file and any other *.less file in the stylesheets directory 
  def customLessEntryPoints(base: File): PathFinder = ( 
      (base / "app" / "assets" / "css" / "bootstrap" * "bootstrap.less") +++
      (base / "app" / "assets" / "css" * "*.less")
  )
  def customJSEntryPoints(base: File): PathFinder = (
    base / "app" / "assets" / "js" ** "*.js"
  )
  
  val main = play.Project(appName, appVersion, appDependencies).settings(
    lessEntryPoints <<= baseDirectory(customLessEntryPoints),
    javascriptEntryPoints <<= baseDirectory(customJSEntryPoints)
    //templatesImport += "org.bson.types.ObjectId",
  )
  
}
