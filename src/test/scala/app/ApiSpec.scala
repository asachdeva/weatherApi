package app

import cats.data.Kleisli
import cats.effect.IO
import cats.implicits._
import eu.timepit.refined.auto._
import eu.timepit.refined.types.net.PortNumber
import eu.timepit.refined.types.string.NonEmptyString
import org.http4s.Charset
import org.http4s.MediaType._
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status
import org.http4s.dsl.io._
import org.http4s.headers.{`Content-Type`, Location}
import org.http4s.implicits._
import io.circe.Json
import io.circe.literal._
import sttp.model.UriInterpolator
import io.odin.Level
import io.odin.json.Formatter
import munit.CatsEffectSuite
import net.asachdeva.BuildInfo

class ApiSpec extends ApiSuite {

  test("GET /weather ") {
    val response = api().run(Request[IO](method = GET, uri = uri"/weather?lat=19.07&lon=72.87"))
    check(response, Ok, expectedContentType = response.unsafeRunSync().contentType, evaluateBody = false)
  }

  test("GET /info should respond with application information") {
    val response = api().run(Request[IO](method = GET, uri = uri"/info"))
    check(
      response,
      Ok,
      expectedBody = json"""
      {
        "name": ${BuildInfo.name},
        "version": ${BuildInfo.version},
        "vmVersion": ${System.getProperty("java.vm.version")},
        "scalaVersion": ${BuildInfo.scalaVersion},
        "sbtVersion": ${BuildInfo.sbtVersion},
        "builtAt": ${BuildInfo.builtAtString},
        "dependencies": ${BuildInfo.test_libraryDependencies.sorted}
      }"""
    )
  }

  test("GET / should redirect to /docs endpoint") {
    val response = api().run(Request[IO](method = GET, uri = uri"/"))
    check(response, PermanentRedirect)
    response.map(r => assertEquals(r.headers.get(`Location`), Some(Location(uri"/docs"))))
  }
}

trait ApiSuite extends CatsEffectSuite {
  def api(): Kleisli[IO, Request[IO], Response[IO]] = {
    val apiDocsConfig = ApiDocsConfig(serverUrl = "http://localhost:8080", description = None)
    val serverConfig: ServerConfig =
      ServerConfig(
        port = PortNumber(8080),
        apiKey = NonEmptyString("cb6f1b8f248f586d6272a58677fc47b6"),
        weatherApi = UriInterpolator.interpolate(StringContext("https://api.openweathermap.org/data/2.5/onecall")),
        apiDocs = apiDocsConfig
      )
    val loggerConfig = LoggerConfig(Level.Debug, Formatter.json)
    Api[IO](Config(serverConfig, loggerConfig))
  }

  def check(responseIO: IO[Response[IO]], expectedStatus: Status, expectedBody: Json): IO[Unit] =
    check(responseIO, expectedStatus, Some(expectedBody.noSpaces), Some(`Content-Type`(application.json)))

  def check(responseIO: IO[Response[IO]], expectedStatus: Status, expectedBody: String): IO[Unit] =
    check(responseIO, expectedStatus, Some(expectedBody), Some(`Content-Type`(text.plain, Charset.`UTF-8`)))

  def check(
    io: IO[Response[IO]],
    expectedStatus: Status,
    expectedBody: Option[String] = None,
    expectedContentType: Option[`Content-Type`] = None,
    evaluateBody: Boolean = true
  ): IO[Unit] = io.flatMap { response =>
    assertEquals(response.status, expectedStatus)
    if (evaluateBody) {
      assertEquals(response.headers.get(`Content-Type`), expectedContentType)
      response.as[String].assertEquals(expectedBody.getOrElse(""))
    } else {
      IO.unit
    }
  }
}
