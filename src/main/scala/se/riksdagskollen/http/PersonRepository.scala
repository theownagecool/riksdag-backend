package se.riksdagskollen.http

import org.json4s.jackson.JsonMethods
import org.json4s.{CustomSerializer, DefaultFormats, Formats, JField, JInt, JObject, JString, JValue}
import se.riksdagskollen.app.Person

import scala.concurrent.{ExecutionContext, Future}

class PersonRepository(client: HttpClientTrait, context: ExecutionContext) {

  implicit val formats: Formats = DefaultFormats + PersonRepository.serializer

  val request = Request(
    "GET",
    "https://data.riksdagen.se/personlista/",
    Seq(
      "Accept" -> "application/json; charset=utf-8",
      "Content-Type" -> "application/json; charset=utf-8"
    ),
    Seq(
      "utformat" -> "json",
      "rdlstatus" -> "samtliga"
    )
  )

  def fetch(): Future[Seq[Person]] = {
    implicit val ec = context
    client.send(request) map { res =>
      val json = JsonMethods.parse(res.body)
      val people = (json \ "personlista" \ "person").extract[Seq[Person]]
      people
    }
  }

}

object PersonRepository {
  private implicit val defaultFormats: Formats = DefaultFormats
  val serializer = new CustomSerializer[Person](format => ({
    case json: JValue =>
      Person(
        (json \ "intressent_id").extract[String],
        (json \ "fodd_ar").extract[String].toInt,
        Person.Gender.parse((json \ "kon").extract[String]),
        (json \ "tilltalsnamn").extract[String],
        (json \ "efternamn").extract[String],
        (json \ "status").extract[String],
        (json \ "parti").extract[String]
      )
  }, {
    case person: Person => JObject(
      JField("person_id", JString(person.id)),
      JField("birth_year", JInt(person.birthYear)),
      JField("gender", JInt(person.gender)),
      JField("first_name", JString(person.firstName)),
      JField("last_name", JString(person.lastName)),
      JField("party", JString(person.party)),
      JField("status", JString(person.status))
    )
  }))
}