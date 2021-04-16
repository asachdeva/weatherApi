import com.scalapenos.sbt.prompt.SbtPrompt.autoImport._
import com.scalapenos.sbt.prompt._
import Dependencies._

// Reload Sbt on changes to sbt or dependencies
Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion := "2.13.5"
ThisBuild / startYear := Some(2021)
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "net.asachdeva"
ThisBuild / organizationName := "asachdeva"

ThisBuild / scalafixDependencies += Libraries.organizeImports
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

val scalafixCommonSettings = inConfig(IntegrationTest)(scalafixConfigSettings(IntegrationTest))

promptTheme := PromptTheme(
  List(
    text(_ => "[weather-api]", fg(64)).padRight(" λ ")
  )
)

lazy val testSettings: Seq[Def.Setting[_]] = List(
  Test / parallelExecution := false,
  (publish / skip) := true,
  fork := true
)

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  (publish / skip) := true
)

lazy val `weather` = project
  .in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    testSettings,
    name := "weatherApi",
    scalacOptions ++= List(
      "-Ymacro-annotations",
      "-Ywarn-unused:imports",
      "-Ywarn-unused:locals",
      "-Yrangepos",
      "-Wconf:cat=unused:info",
      "-Xmacro-settings:materialize-derivations"
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    scalafmtOnCompile := true,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, Test / libraryDependencies),
    buildInfoPackage := organization.value,
    buildInfoOptions ++= Seq[BuildInfoOption](BuildInfoOption.BuildTime),
    Defaults.itSettings,
    scalafixCommonSettings,
    libraryDependencies ++= Seq(
      CompilerPlugin.kindProjector,
      CompilerPlugin.betterMonadicFor,
      CompilerPlugin.semanticDB,
      Libraries.circeCore,
      Libraries.circeGeneric,
      Libraries.circeGenericExtras,
      Libraries.circeLiteral,
      Libraries.circeParser,
      Libraries.circeRefined,
      Libraries.cirisCore,
      Libraries.cirisRefined,
      Libraries.http4sDsl,
      Libraries.http4sServer,
      Libraries.http4sCirce,
      Libraries.logback % Runtime,
      Libraries.odinCore,
      Libraries.odinJson,
      Libraries.odinSlf4J,
      Libraries.tapirCirce,
      Libraries.tapirClient,
      Libraries.tapirCore,
      Libraries.tapirHttp4s,
      Libraries.tapirOpenApiCirce,
      Libraries.tapirOpenApiDocs,
      Libraries.tapirRefined,
      Libraries.tapirSwagger,
      Libraries.munit,
      Libraries.munitCE
    )
  )

// Format and scalafix
addCommandAlias("format", ";scalafmtAll ;scalafmtSbt ;scalafixAll")

// CI build
addCommandAlias("buildWeatherApi", ";clean;+test;")
