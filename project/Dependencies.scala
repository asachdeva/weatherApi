import sbt._

object Dependencies {

  object V {
    val circe  = "0.13.0"
    val ciris  = "1.2.1"
    val http4s = "0.21.22"
    val tapir  = "0.18.0-M5"

    val betterMonadicFor = "0.3.1"
    val kindProjector    = "0.11.3"

    val logback = "1.2.3"
    val odin    = "0.11.0"

    val organizeImports = "0.5.0"
    val semanticDB      = "4.4.13"

    val munit   = "0.7.23"
    val munitCE = "1.0.1"
  }

  object Libraries {
    def circe(artifact: String): ModuleID  = "io.circe"                    %% s"circe-$artifact"  % V.circe
    def ciris(artifact: String): ModuleID  = "is.cir"                      %% s"$artifact"        % V.ciris
    def http4s(artifact: String): ModuleID = "org.http4s"                  %% s"http4s-$artifact" % V.http4s
    def odin(artifact: String): ModuleID   = "com.github.valskalla"        %% s"odin-$artifact"   % V.odin
    def tapir(artifact: String): ModuleID  = "com.softwaremill.sttp.tapir" %% s"tapir-$artifact"  % V.tapir

    val circeCore          = circe("core")
    val circeParser        = circe("parser")
    val circeGeneric       = circe("generic")
    val circeGenericExtras = circe("generic-extras")
    val circeLiteral       = circe("literal")
    val circeOptics        = circe("optics")
    val circeRefined       = circe("refined")

    val cirisCore    = ciris("ciris")
    val cirisRefined = ciris("ciris-refined")

    val tapirCirce        = tapir("json-circe")
    val tapirClient       = tapir("sttp-client")
    val tapirCore         = tapir("core")
    val tapirHttp4s       = tapir("http4s-server")
    val tapirOpenApiCirce = tapir("openapi-circe-yaml")
    val tapirOpenApiDocs  = tapir("openapi-docs")
    val tapirRefined      = tapir("refined")
    val tapirSwagger      = tapir("swagger-ui-http4s")

    val http4sDsl    = http4s("dsl")
    val http4sServer = http4s("blaze-server")
    val http4sCirce  = http4s("circe")

    val odinCore  = odin("core")
    val odinJson  = odin("json")
    val odinSlf4J = odin("slf4j")

    // Runtime
    lazy val logback = "ch.qos.logback" % "logback-classic" % V.logback

    // Test
    val munit   = "org.scalameta" %% "munit"               % V.munit   % Test
    val munitCE = "org.typelevel" %% "munit-cats-effect-2" % V.munitCE % Test

    // Scalafix rules
    val organizeImports = "com.github.liancheng" %% "organize-imports" % V.organizeImports
  }

  object CompilerPlugin {

    val betterMonadicFor = compilerPlugin(
      "com.olegpy" %% "better-monadic-for" % V.betterMonadicFor
    )

    val kindProjector = compilerPlugin(
      ("org.typelevel" % "kind-projector" % V.kindProjector).cross(CrossVersion.full)
    )

    val semanticDB = compilerPlugin(
      ("org.scalameta" % "semanticdb-scalac" % V.semanticDB).cross(CrossVersion.full)
    )
  }

}
