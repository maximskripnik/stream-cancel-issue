import java.util.UUID

import cats.effect.concurrent.Deferred
import cats.effect.{Concurrent, Timer}
import cats.syntax.flatMap._
import cats.syntax.applicativeError._
import fs2.Stream
import org.http4s.{EntityEncoder, HttpRoutes}
import org.http4s.dsl.Http4sDsl

import scala.concurrent.duration._

class TestController[F[_] : Concurrent : Timer](deferred: Deferred[F, Unit]) extends Http4sDsl[F] {

  private implicit val uuidEncoder: EntityEncoder[F, UUID] =
    EntityEncoder
        .stringEncoder[F]
        .contramap(_.toString + "\n")

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "stream" =>
      val stream = Stream
        .repeatEval(Concurrent[F].delay(UUID.randomUUID()).flatTap(_ => Timer[F].sleep(2.seconds)))
        .interruptWhen(deferred.get.attempt)
      Ok(stream)
    case POST -> Root / "complete" =>
      deferred.complete(()).flatMap(_ => Ok("stream is cancelled"))
  }

}

