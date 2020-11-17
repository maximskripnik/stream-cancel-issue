import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect.concurrent.Deferred
import cats.effect.{Concurrent, IO, Timer}
import cats.~>
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

object Routes {

  def createRoutes(
      deferred1: Deferred[IO, Unit],
      deferred2: Deferred[IO, Unit]
  )(implicit c: Concurrent[IO], timer: Timer[IO]): HttpRoutes[IO] = {

    type SpecialEff[T] = EitherT[IO, String, T]

    val ioToSpecial: IO ~> SpecialEff = EitherT.liftK
    val specialToIO: SpecialEff ~> IO =
      new ~>[SpecialEff, IO] {
        override def apply[A](fa: SpecialEff[A]): IO[A] = fa.value.flatMap {
          case Left(_)    => IO.raiseError(new RuntimeException("should never happen"))
          case Right(res) => IO.pure(res)
        }
      }

    val dsl = Http4sDsl[IO]
    import dsl._

    val ioTestRoutes: HttpRoutes[IO] = new TestController[IO](deferred1).routes
    val specialTestRoutes: HttpRoutes[SpecialEff] =
      new TestController[SpecialEff](deferred2.mapK(ioToSpecial)).routes

    val convertedSpecialRoutes: HttpRoutes[IO] = Kleisli { requestIO =>
      val requestSpecial =
        Request[SpecialEff]( // no mapK cause it uses Stream#translate for the body (which makes stream uninterruptible)
          method = requestIO.method,
          uri = requestIO.uri,
          httpVersion = requestIO.httpVersion,
          headers = requestIO.headers,
          body = requestIO.body.translateInterruptible(ioToSpecial),
          attributes = requestIO.attributes
        )

      val specialResult: OptionT[SpecialEff, Response[SpecialEff]] =
        specialTestRoutes(requestSpecial)

      val ioResult: IO[Option[Response[IO]]] = specialResult.value
          .map(_.map { specialResponse =>
            Response[IO]( // no mapK cause it uses Stream#translate for the body (which makes stream uninterruptible)
              status = specialResponse.status,
              httpVersion = specialResponse.httpVersion,
              headers = specialResponse.headers,
              body = specialResponse.body.translateInterruptible(specialToIO)
            )
          })
          .value
          .flatMap {
            case Right(maybeResp) =>
              IO.pure(maybeResp)
            case Left(error) =>
              BadRequest(error).map(Some(_))
          }

      OptionT(ioResult)
    }

    Router[IO]("/io" -> ioTestRoutes, "/special" -> convertedSpecialRoutes)
  }
}

