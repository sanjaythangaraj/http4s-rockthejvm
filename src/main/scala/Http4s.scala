import cats.data.{Kleisli, OptionT}
import cats.*
import cats.effect.*
import cats.implicits.*
import org.http4s.circe.*
import org.http4s.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.headers.*
import org.http4s.implicits.*
import org.http4s.server.*

import java.time.Year
import scala.collection.mutable
import scala.util.Try

object Http4s {

  type Actor = String
  case class Movie(id: String, title: String, year: Int, actors: List[Actor], director: String)
  case class Director(firstName: String, lastName: String) {
    override def toString: String = s"$firstName $lastName"
  }
  case class DirectorDetails(firstName: String,  lastName: String, genre: String )

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
    QueryParamDecoder[Int].map(yearInt => Year.of(yearInt))

  object DirectorQueryParamMatcher extends QueryParamDecoderMatcher[String]("director")
  object YearQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Year]("year")

  def movieRoutes[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "movies" :? DirectorQueryParamMatcher(director) +& YearQueryParamMatcher(year) => ???
      case GET -> Root / "movies" / UUIDVar(movieId) / "actors" => ???
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
      case GET -> Root / "directors" / DirectorPath(director) => ???
    }
  }

  def allRoutes[F[_]: Monad]: HttpRoutes[F] =
    movieRoutes[F] <+> directorRoutes[F] // import cats.syntax.semigroupk._

  def allRoutesComplete[F[_]: Monad]: HttpApp[F] =
    allRoutes[F].orNotFound
}
