name := "ip-getter-cli"

version := "0.1"

scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "dev.zio"      %% "zio-http"          % "3.0.0-RC6",

  "dev.zio"      %% "zio-test"          % "2.1-RC1" % Test,
  "dev.zio"      %% "zio-test-sbt"      % "2.1-RC1" % Test,
  "dev.zio"      %% "zio-test-magnolia" % "2.1-RC1" % Test,
  "org.wiremock" %  "wiremock"          % "3.5.2"   % Test
)

ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF","io.netty.versions.properties") =>  MergeStrategy.discard
  case x   =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}