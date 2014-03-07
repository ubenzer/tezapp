import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "tezapp"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "commons-codec" % "commons-codec" % "1.9",
    "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2",

    "org.openrdf.sesame" % "sesame-rio"           % "2.7.10",
    "org.openrdf.sesame" % "sesame-rio-api"       % "2.7.10",
    "org.openrdf.sesame" % "sesame-rio-trig"      % "2.7.10",
    "org.openrdf.sesame" % "sesame-rio-trix"      % "2.7.10",
    "org.openrdf.sesame" % "sesame-rio-rdfxml"    % "2.7.10",
    "org.openrdf.sesame" % "sesame-rio-rdfjson"   % "2.7.10",
    "org.openrdf.sesame" % "sesame-rio-nquads"    % "2.7.10",
    "org.openrdf.sesame" % "sesame-rio-ntriples"  % "2.7.10",
    "org.openrdf.sesame" % "sesame-rio-n3"        % "2.7.10",
    "org.openrdf.sesame" % "sesame-rio-turtle"    % "2.7.10",
    "org.openrdf.sesame" % "sesame-rio-binary"    % "2.7.10",
    "org.openrdf.sesame" % "sesame-rio-datatypes" % "2.7.10",
    "org.openrdf.sesame" % "sesame-rio-languages" % "2.7.10"
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
}
