package se.riksdagskollen.http

import java.sql.Timestamp

import org.json4s.{CustomSerializer, DefaultFormats, Formats, JField, JObject, JString, JValue, JInt}
import se.riksdagskollen.app.{Vote, Voting}

import scala.concurrent.{ExecutionContext, Future}

class VotingRepository(httpClient: HttpClientTrait, context: ExecutionContext) {

  implicit val formats: Formats = DefaultFormats ++ Seq(
    VotingRepository.voteSerializer
  )

  val listRequest = Request(
    "GET",
    "https://data.riksdagen.se/voteringlista/",
    Seq(
      "Accept" -> "application/json; charset=utf-8",
      "Content-Type" -> "application/json; charset=utf-8"
    ),
    Seq(
      "utformat" -> "json",
      "gruppering" -> "votering_id",
      "sz" -> "100000"
    )
  )

  val votingRequest = (id: String) => Request(
    "GET",
    s"https://data.riksdagen.se/votering/$id/json",
    Seq(
      "Accept" -> "application/json; charset=utf-8",
      "Content-Type" -> "application/json; charset=utf-8"
    )
  )

  def fetchVotingIds(year: Int): Future[Seq[String]] = {
    implicit val ec = context
    val rm = year + "/" + (year+1).toString.takeRight(2) // 2015/16
    val req = listRequest.copy(query = listRequest.query ++ Seq("rm" -> rm))
    println(req)
    httpClient.send(req) map { res =>
      (res.json \ "voteringlista" \ "votering" \ "votering_id").extract[Seq[String]]
    }
  }

  def fetch(id: String): Future[(Voting, Seq[Vote])] = {
    implicit val ec = context
    httpClient.send(votingRequest(id)) map { res =>
      val date = Timestamp.valueOf((res.json \ "votering" \ "dokument" \ "datum").extract[String])
      val title = (res.json \ "votering" \ "dokument" \ "titel").extract[String]
      val voting = Voting(id, date, title)
      val votes = (res.json \ "votering" \ "dokvotering" \ "votering").extract[Seq[Vote]]
      (voting, votes)
    }
  }

}

object VotingRepository {

  private implicit val defaultFormats: Formats = DefaultFormats

  val voteSerializer = new CustomSerializer[Vote](formats => (
    {
      case x: JValue =>
        Vote(
          Vote.Value.parse((x \ "rost").extract[String]),
          Vote.Regarding.parse((x \ "avser").extract[String]),
          (x \ "votering_id").extract[String],
          (x \ "intressent_id").extract[String]
        )
    },
    {
      case x: Vote =>
        JObject(
          JField("value", JInt(x.value)),
          JField("regarding", JInt(x.regarding))
        )
    }
    ))

}