logLevel := Level.Warn

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")
addSbtPlugin("io.crashbox" % "sbt-gpg" % "0.2.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "1.0.0")
//addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")
addDependencyTreePlugin
