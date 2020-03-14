import java.io.{BufferedReader, InputStreamReader}

lazy val xmlCalabashVersion = "1.99.13"
lazy val jafplVersion = "0.2.41"
lazy val saxonVersion = "9.9.1-7"
lazy val useSaxonEE = true

name         := "XML Calabash"
organization := "com.xmlcalabash"
homepage     := Some(url("https://xmlcalabash.com/"))
version      := xmlCalabashVersion
scalaVersion := "2.12.6"

buildInfoUsePackageAsPath := true
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
        case _: Exception => "FAILED"
      }
    }}.toString()
)

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion),
    buildInfoPackage := "com.xmlcalabash.sbt"
  )

lazy val debugSbtTask = taskKey[Unit]("Task for debugging things I don't understand about sbt")
debugSbtTask := {
  println(s"unmanaged: $unmanagedBase")
}

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
      case _: Exception => "FAILED"
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
      case _: Exception => "FAILED"
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


resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"
resolvers += "Restlet" at "http://maven.restlet.com"

libraryDependencies ++= Seq(
  //"org.apache.logging.log4j" % "log4j-api" % "2.12.1",
  //"org.apache.logging.log4j" % "log4j-core" % "2.12.1",
  //"org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.12.1",
  //"org.slf4j" % "jcl-over-slf4j" % "1.7.25",
  //"org.slf4j" % "slf4j-api" % "1.7.25",
  "org.apache.commons" % "commons-compress" % "1.19",
  "com.typesafe.akka" %% "akka-actor" % "2.5.25",
  //"com.typesafe.akka" %% "akka-slf4j" % "2.5.25",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  //"ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.13" % Test,
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.1.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.0",
  "org.scala-lang.modules" %% "scala-swing" % "2.0.3",
  "com.ibm.icu" % "icu4j" % "59.1",
  "org.apache.httpcomponents" % "httpclient" % "4.5.11",
  "org.apache.httpcomponents" % "httpcore" % "4.4.13",
  "org.apache.httpcomponents" % "httpmime" % "4.5.11",
  "org.restlet.jee" % "org.restlet" % "2.2.2",
  "org.restlet.jee" % "org.restlet.ext.fileupload" % "2.2.2",
  "org.restlet.jee" % "org.restlet.ext.slf4j" % "2.2.2",
  "org.xmlresolver" % "xmlresolver" % "0.13.1",
  "org.relaxng" % "jing" % "20181222",
  "nu.validator" % "htmlparser" % "1.4.12",
  "com.atlassian.commonmark" % "commonmark" % "0.12.1",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.10.2",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.2",
  "com.jafpl" % "jafpl_2.12" % jafplVersion
)

lazy val logback = "ch.qos.logback" % "logback-classic" % "1.0.13"

libraryDependencies ++= (
  if (!useSaxonEE) {
    Seq("net.sf.saxon" % "Saxon-HE" % saxonVersion)
  } else {
    List()
  }
)

// ============================================================
// This section is an attempt to get sbt assembly to work.
// It's a bit of trial and error more than informed choice.

assemblyJarName in assembly := Array("xml-calabash",
  xmlCalabashVersion,
  saxonVersion.split("\\.").take(2).mkString("")).mkString("-") + ".jar"

test in assembly := {}

libraryDependencies +=
  "org.relaxng" % "jing" % "20181222" excludeAll(
    ExclusionRule(organization = "com.sun.xml.bind.jaxb"),
    ExclusionRule(organization = "isorelax"),
    ExclusionRule(organization = "relaxngDatatype")
  )

libraryDependencies +=
  "org.apache.httpcomponents" % "httpclient" % "4.5.3" excludeAll(
    ExclusionRule(organization = "commons-logging")
  )

dependencyOverrides += "net.sf.saxon" % "Saxon-HE" % saxonVersion
dependencyOverrides += "xml-apis" % "xml-apis" % "1.3.04"

mappings in (Compile, packageBin) := {
  (mappings in (Compile, packageBin)).value.filter {
    case (file, toPath) => toPath != "com/xmlcalabash/drivers/Test.class"
  }
}

// ============================================================

unmanagedJars in Compile ++= (
  if (useSaxonEE) {
    Seq(file(s"${baseDirectory.value}/eelib/$saxonVersion/saxon9ee.jar"))
  } else {
    Seq()
  }
)

unmanagedJars in Runtime ++= (
  if (useSaxonEE) {
    Seq(file(s"${baseDirectory.value}/eelib/$saxonVersion/saxon9ee.jar"))
  } else {
    Seq()
  }
)

unmanagedClasspath in Runtime ++= (
  if (useSaxonEE) {
    Seq(file(s"${baseDirectory.value}/eelib"))
  } else {
    Seq()
  }
)

// Yes, this is an odd place for local use, but it's where the website
// needs them. I should figure out how to parameterize the location...
//target in Compile in doc := baseDirectory.value / "build/pages/apidocs"
//scalacOptions in (Compile, doc) ++= Seq(
//  "-doc-root-content", baseDirectory.value+"/docs/apidocs/root.md"
//)

scalacOptions := Seq("-unchecked", "-deprecation")
javacOptions := Seq("-Xlint:unchecked", "-Xlint:deprecation")
