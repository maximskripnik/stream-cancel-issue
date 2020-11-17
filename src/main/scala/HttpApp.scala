import cats.effect.concurrent.Deferred
import cats.effect.{ ExitCode, IO, IOApp }
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._

object HttpApp extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      deferred1 <- Deferred[IO, Unit]
      deferred2 <- Deferred[IO, Unit]
      routes = Routes.createRoutes(deferred1 = deferred1, deferred2 = deferred2)
      _ <- BlazeServerBuilder[IO]
          .bindHttp(8080, "0.0.0.0")
          .withHttpApp(routes.orNotFound)
          .serve
          .compile
          .drain
    } yield ExitCode.Success

}
