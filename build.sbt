ThisBuild / scalaVersion := "2.13.18"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, DebianPlugin, SystemdPlugin)
  .settings(
    name    := "email-extractor-scala",
    version := "0.0.9",
    libraryDependencies ++= Seq(
      guice,
      "org.webjars" %% "webjars-play" % "3.0.10",
      "org.webjars"  % "jquery"       % "3.7.1",
      "org.webjars"  % "bootstrap"    % "3.4.1",
      "org.webjars"  % "prettify"     % "4-Mar-2013-1",
      "org.webjars"  % "html5shiv"    % "3.7.3",
      "qa.hedgehog" %% "hedgehog-sbt" % "0.15.1" % Test,
    ),
    testFrameworks += TestFramework("hedgehog.sbt.Framework"),
    TwirlKeys.templateImports += "models._",
    Assets / LessKeys.less / includeFilter := "*.less",
    LessKeys.compress         := true,
    Assets / pipelineStages   := Seq(uglify),
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlint", "-Wconf:src=routes/.*:s,src=twirl/.*:s"),
    Linux / maintainer        := "Kevin Lee <kevin@kevinlee.io>",
    Linux / packageSummary    := "Email Address Extractor",
    packageDescription        := "Email Address Extractor app",
    debianPackageDependencies := Seq("openjdk-21-jre-headless"),
    Debian / version          := sys.env.get("BUILD_NUMBER").fold(version.value)(n => s"${version.value}+$n"),
    Universal / javaOptions ++= Seq(
      "-J-Xmx192m",
      "-Dhttp.address=127.0.0.1",
      "-Dpidfile.path=/dev/null",
      "-Dplay.http.context=/email-extractor-scala",
    ),
  )
