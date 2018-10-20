import java.io.{BufferedReader, InputStreamReader}

name         := "XML Calabash"
organization := "com.xmlcalabash"
version      := "1.9.16"
scalaVersion := "2.12.6"

lazy val jafplVersion = "0.0.67"

buildInfoKeys ++= Seq[BuildInfoKey](
  "jafplVersion" -> jafplVersion,
  BuildInfoKey.action("buildTime") {
    System.currentTimeMillis
  },
  // Hat tip to: https://stackoverflow.com/questions/24191469/how-to-add-commit-hash-to-play-templates
  "gitHash" -> new java.lang.Object() {
    override def toString: String = {
      try {
        val extracted = new InputStreamReader(
          java.lang.Runtime.getRuntime.exec("git rev-parse HEAD").getInputStream
        )
        new BufferedReader(extracted).readLine
      } catch {
        case ex: Exception => "FAILED"
      }
    }}.toString()
)

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion),
    buildInfoPackage := "com.xmlcalabash.sbt"
  )

lazy val failTask = taskKey[Unit]("Force the build to fail")
failTask := {
  throw new sbt.MessageOnlyException("No build for you.")
}

// Redefine publish so that it will fail if the repo is dirty
publish := Def.taskDyn {
  val default = publish.taskValue

  val shortstat = {
    try {
      val extracted = new InputStreamReader(
        java.lang.Runtime.getRuntime.exec("git diff --shortstat").getInputStream
      )
      var diff = ""
      val reader = new BufferedReader(extracted)
      var line = reader.readLine
      while (line != null) {
        diff = line
        line = reader.readLine
      }
      reader.close()
      diff
    } catch {
      case ex: Exception => "FAILED"
    }
  }

  val status = {
    try {
      val extracted = new InputStreamReader(
        java.lang.Runtime.getRuntime.exec("git status --porcelain").getInputStream
      )
      var newFile = ""
      val reader = new BufferedReader(extracted)
      var line = reader.readLine
      while (line != null) {
        if (line.startsWith("??")) {
          newFile = line
        }
        line = reader.readLine
      }
      reader.close()
      newFile
    } catch {
      case ex: Exception => "FAILED"
    }
  }

  val message = if (shortstat != "") {
    if (status != "") {
      Some("Repository has changed and untracked files.")
    } else {
      Some("Repository has changed files.")
    }
  } else if (status != "") {
    Some("Repository has untracked files.")
  } else {
    None
  }

  if (message.isDefined) {
    println(message.get)
  }

  if (message.isDefined) {
    Def.taskDyn {
      failTask
    }
  } else {
    Def.task(default.value)
  }
}.value

resolvers += DefaultMavenRepository
resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"
resolvers += "Restlet" at "http://maven.restlet.com"
resolvers += "My Maven Repository" at "https://nwalsh.com/maven/repo"
resolvers += "Local Maven Repository" at "file:///space/websites/nwalsh.com/build/website/maven/repo"

libraryDependencies ++= Seq(
  "org.apache.logging.log4j" % "log4j-api" % "2.11.0",
  "org.apache.logging.log4j" % "log4j-core" % "2.11.0",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.11.0",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.25",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "com.typesafe.akka" %% "akka-actor" % "2.5.13",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.13" % Test,
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.1.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.0",
  "org.scala-lang.modules" %% "scala-swing" % "2.0.3",
  "net.sf.saxon" % "Saxon-HE" % "9.8.0-14",
  //"com.thaiopensource" % "jing" % "20091111",
  "net.java.dev.msv" % "msv-core" % "2013.6.1",
  "com.ibm.icu" % "icu4j" % "59.1",
  "org.apache.httpcomponents" % "httpclient" % "4.5.3",
  "org.apache.httpcomponents" % "httpcore" % "4.4.6",
  "org.restlet.jee" % "org.restlet" % "2.2.2",
  "org.restlet.jee" % "org.restlet.ext.fileupload" % "2.2.2",
  "org.restlet.jee" % "org.restlet.ext.slf4j" % "2.2.2",
  "org.xmlresolver" % "xmlresolver" % "0.12.3",
  "nu.validator" % "htmlparser" % "1.4.6",
  "com.jafpl" % "jafpl_2.12" % jafplVersion
)

// Yes, this is an odd place for local use, but it's where the website
// needs them. I should figure out how to parameterize the location...
//target in Compile in doc := baseDirectory.value / "build/pages/apidocs"
//scalacOptions in (Compile, doc) ++= Seq(
//  "-doc-root-content", baseDirectory.value+"/docs/apidocs/root.md"
//)

scalacOptions := Seq("-unchecked", "-deprecation")

// I'm publishing the informal pre-release builds on my own repo
publishTo := Some(Resolver.file("file",
  new File("/space/websites/nwalsh.com/build/website/maven/repo")))
