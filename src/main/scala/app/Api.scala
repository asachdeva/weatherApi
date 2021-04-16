package app

import cats.Applicative
import cats.data.Kleisli
import cats.effect._
import cats.implicits._

import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.collection._

import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location
import org.http4s.implicits._
import org.http4s.server.middleware.CORS

import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._

import sttp.client3.HttpURLConnectionBackend
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.apispec.Tag
import sttp.tapir.client.sttp._
import sttp.tapir.docs.openapi._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.openapi.Server
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s._
import sttp.tapir.swagger.http4s.SwaggerHttp4s

import net.asachdeva.BuildInfo

final case class Kelvin(value: Double) extends AnyVal
final case class WeatherCondition(main: String, description: String)
final case class CurrentConditions(feelsLike: Kelvin, weather: List[WeatherCondition])
final case class Alert(description: String)
final case class WeatherStatus(current: CurrentConditions, alerts: List[Alert] = List.empty)
case class WeatherReport(weatherConditions: List[String], summary: String, alerts: List[String])

object WeatherReport {
  implicit val customConfig: Configuration           = Configuration.default.withDefaults
  implicit val reportEncoder: Encoder[WeatherReport] = deriveConfiguredEncoder
}

object WeatherStatus {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults

  implicit val kelvinDecoder: Decoder[Kelvin]                       = deriveUnwrappedDecoder
  implicit val weatherConditionDecoder: Decoder[WeatherCondition]   = deriveConfiguredDecoder
  implicit val currentConditionsDecoder: Decoder[CurrentConditions] = deriveConfiguredDecoder
  implicit val alertDecoder: Decoder[Alert]                         = deriveConfiguredDecoder
  implicit val weatherStatusDecoder: Decoder[WeatherStatus]         = deriveConfiguredDecoder

  implicit def statusEntityDecoder[F[_]: Sync]: EntityDecoder[F, WeatherStatus] = jsonOf
}

object Api {

  def apply[F[_]: Concurrent: ContextShift: Timer](config: Config): Kleisli[F, Request[F], Response[F]] = {

    val dsl = Http4sDsl[F]
    import dsl._

    val apis: List[TapirApi[F]] = List(WeatherApi(config))

    val docs: OpenAPI = OpenAPIDocsInterpreter
      .toOpenAPI(
        apis.flatMap(_.endpoints),
        openapi.Info(BuildInfo.name, BuildInfo.version, config.server.apiDocs.description)
      )
      .servers(List(Server(config.server.apiDocs.serverUrl)))
      .tags(apis.map(_.tag))

    val redirectRootToDocs = HttpRoutes.of[F] { case path @ GET -> Root =>
      PermanentRedirect(Location(path.uri / "docs"))
    }

    val routes: List[HttpRoutes[F]] =
      apis.map(_.routes) ++ List(new SwaggerHttp4s(docs.toYaml).routes, redirectRootToDocs)

    CORS(routes.reduce(_ <+> _)).orNotFound
  }
}

object WeatherApi {

  def apply[F[_]: Concurrent: ContextShift: Timer](config: Config)(implicit F: Applicative[F]) =
    new TapirApi[F] {
      override val tag                  = Tag("WeatherAPI", None)
      override lazy val serverEndpoints = List(info, weather)
      val backend                       = HttpURLConnectionBackend()
      type NonEmptyString = String Refined NonEmpty

      private val info: ServerEndpoint[Unit, StatusCode, Info, Any, F] =
        endpoint.get
          .summary("Fetch general information about the WeatherApi application")
          .tag(tag.name)
          .in("info")
          .out(jsonBody[Info])
          .errorOut(statusCode)
          .serverLogic(_ =>
            F.pure(
              Info(
                BuildInfo.name,
                BuildInfo.version,
                System.getProperty("java.vm.version"),
                BuildInfo.scalaVersion,
                BuildInfo.sbtVersion,
                BuildInfo.builtAtString,
                BuildInfo.test_libraryDependencies.sorted
              ).asRight
            )
          )

      private val kelvinToString: Kelvin => String = {
        case Kelvin(x) if x < 260.0               => "Crazy Cold"
        case Kelvin(x) if x < 295.0 && x >= 260.0 => "Cold"
        case Kelvin(x) if x > 302.0 && x < 310.0  => "Hot"
        case Kelvin(x) if x >= 310.0              => "Crazy Hot"
        case _                                    => "Moderate"
      }

      val makeCall = endpoint.get
        .in(query[String]("lat"))
        .in(query[String]("lon"))
        .in(query[String]("appid"))
        .in(query[String]("exclude"))
        .out(jsonBody[WeatherStatus])
        .errorOut(statusCode)

      def getWeather(coordinates: (Option[Double], Option[Double])): F[Either[StatusCode, WeatherReport]] = {
        val (latitude, longitude) = coordinates

        val request = SttpClientInterpreter.toRequestThrowDecodeFailures(makeCall, config.server.weatherApi.some)

        val response = request
          .apply(
            (
              latitude.get.toString,
              longitude.get.toString,
              config.server.apiKey.toString(),
              "minutely,hourly,daily"
            )
          )
          .send(backend)
        F.pure(response.body.map { status =>
          WeatherReport(
            weatherConditions = status.current.weather.map { case WeatherCondition(main, description) =>
              s"$main: $description"
            },
            summary = kelvinToString(status.current.feelsLike),
            alerts = status.alerts.map(_.description)
          )
        })
      }

      private val weather: ServerEndpoint[(Option[Double], Option[Double]), StatusCode, WeatherReport, Any, F] =
        endpoint.get
          .summary("Weather Api endpoint")
          .tag(tag.name)
          .in("weather")
          .in(query[Option[Double]]("lat").description("Required Latitude"))
          .in(query[Option[Double]]("lon").description("Required Longitude"))
          .out(jsonBody[WeatherReport])
          .errorOut(statusCode)
          .serverLogic { coordinates =>
            getWeather(coordinates)
          }

      case class Info(
        name: String,
        version: String,
        vmVersion: String,
        scalaVersion: String,
        sbtVersion: String,
        builtAt: String,
        dependencies: Seq[String]
      )

    }
}

abstract class TapirApi[F[_]: Concurrent: ContextShift: Timer] {
  def tag: Tag
  def serverEndpoints: List[ServerEndpoint[_, _, _, Any, F]]
  def endpoints: List[Endpoint[_, _, _, _]] = serverEndpoints.map(_.endpoint)
  def routes: HttpRoutes[F]                 = Http4sServerInterpreter.toRoutes(serverEndpoints)
}
