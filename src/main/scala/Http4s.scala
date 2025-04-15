import cats.Monad
import cats.data.{Kleisli, OptionT}
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.*
import com.comcast.ip4s.{ipv4, port}
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.dsl._
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

import java.time.Year
import java.util.UUID
import scala.collection.mutable
import scala.util.Try

object Http4s extends IOApp{

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  type Actor = String
  case class Movie(id: String, title: String, year: Int, actors: List[Actor], director: String)
  case class Director(firstName: String, lastName: String) {
    override def toString: String = s"$firstName $lastName"
  }
  case class DirectorDetails(firstName: String,  lastName: String, genre: String )

  val snjl: Movie = Movie(
    "6bcbca1e-efd3-411d-9f7c-14b872444fce",
    "Zack Snyder's Justice League",
    2021,
    List("Henry Cavill", "Gal Godot", "Ezra Miller", "Ben Affleck", "Ray Fisher", "Jason Momoa"),
    "Zack Snyder"
  )


  val movies: Map[String, Movie] = Map(snjl.id -> snjl)

  private def findMovieById(movieId: UUID) =
    movies.get(movieId.toString)


  private def findMoviesByDirector(director: String): List[Movie] =
    movies.values.filter(_.director == director).toList

  /*
    - GET ALL movies for a director under a given year
    - GET ALL actors for a movie
    - GET details about a director
    - POST add a new director
   */

  // Request[G] -> F[Option[Response[G]]]
  // Request[G] -> OptionT[F, Response[G]]
  // Request[G] -> Kleisli[OptionT[F, *], Request[G], Response[G]].
  // Kleisli[OptionT[F, *], Request[G], Response[G]]

  // type Http[F[_], G[_]] = Kleisli[F, Request[G], Response[G]]

  // type HttpRoutes[F[_]] = Http[OptionT[F, *], F]

  // HttpApp[F[_]] = Http[F, F]

  // Get /movies?director=Zack%20Snyder&year=2021

  given yearQueryParamDecoder: QueryParamDecoder[Year] =
    QueryParamDecoder[Int].emap{yearInt =>
      Try(Year.of(yearInt))
        .toEither
        .leftMap { e =>
          ParseFailure(e.getMessage, e.getMessage)
        }
    }

  object DirectorQueryParamMatcher extends QueryParamDecoderMatcher[String]("director")
  object YearQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[Year]("year")

  def movieRoutes[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "movies" :? DirectorQueryParamMatcher(director) +& YearQueryParamMatcher(maybeYear) =>
        maybeYear match
          case Some(validatedYear) =>
            validatedYear.fold(
              _ => BadRequest("The year was badly formatted"),
              year => {
                val moviesByDirector = findMoviesByDirector(director)
                val moviesByDirectorAndYear = moviesByDirector.filter(_.year == year.getValue)
                Ok(moviesByDirectorAndYear.asJson)
              }
            )
          case None => Ok(findMoviesByDirector(director).asJson)
      case GET -> Root / "movies" / UUIDVar(movieId) / "actors" =>
        findMovieById(movieId).map(_.actors) match
          case Some(actors) => Ok(actors.asJson)
          case _ => NotFound(s"No movie with id $movieId found in the database")
    }
  }

  object DirectorPath {
    def unapply(str: String): Option[Director] = {
      Try {
        val tokens = str.split(" ")
        Director(tokens(0), tokens(1))
      }.toOption
    }
  }

  val directorsDetailsDB: mutable.Map[Director, DirectorDetails] = mutable.Map (
    Director("Zack", "Snyder") -> DirectorDetails("Zack", "Snyder", "superhero")
  )

  def directorRoutes[F[_] : Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "directors" / DirectorPath(director) =>
        directorsDetailsDB.get(director) match
          case Some(directorDetails) => Ok(directorDetails.asJson)
          case _ => NotFound(s"No director '$director' found")
    }
  }

  def allRoutes[F[_]: Monad]: HttpRoutes[F] =
    movieRoutes[F] <+> directorRoutes[F] // import cats.syntax.semigroupk._

  def allRoutesComplete[F[_]: Monad]: HttpApp[F] =
    allRoutes[F].orNotFound

  val serverResource = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"8080")
    .withHttpApp(allRoutesComplete)
    .build

  override def run(args: List[String]): IO[ExitCode] =
    serverResource.use(_ => IO.never).as(ExitCode.Success)
}
