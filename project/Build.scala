import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  resolvers += Resolver.sonatypeRepo("snapshots")
  scalacOptions ++= Seq("-unchecked", "-deprecation","-feature","-warning")

  val appName         = "tezapp"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    javaCore,
    "org.mongodb" %% "casbah-gridfs" % "2.6.1",
    "com.github.nscala-time" %% "nscala-time" % "0.4.0",
    "se.radley" %% "play-plugins-salat" % "1.2",
    "org.openrdf.sesame" % "sesame-runtime" % "2.7.0",
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
  def customJSEntryPoints(base: File): PathFinder = (
    base / "app" / "assets" / "js" ** "*.js"
  )
  
  val main = play.Project(appName, appVersion, appDependencies).settings(
    lessEntryPoints <<= baseDirectory(customLessEntryPoints),
    javascriptEntryPoints <<= baseDirectory(customJSEntryPoints),
    routesImport += "se.radley.plugin.salat.Binders._",
    templatesImport += "org.bson.types.ObjectId",
    resolvers += "Maven Central" at "http://repo1.maven.org/maven2",
    resolvers += "Typesafe Repository 2" at "http://repo.typesafe.com/typesafe/repo/",

    templatesImport += "_root_.views.helper.JSDependency"
  )
  
}
