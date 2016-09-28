package controllers

import javax.inject.Inject

import play.api.mvc._
import play.api.libs.json.Json

class Tickets @Inject()(action: BaseAction) extends Controller {

  case class AvailabilityResponse(result: String, ticketQuantity: Option[Long])
  object AvailabilityResponse {
    implicit val responseFormat = Json.format[AvailabilityResponse]
  }
  def ticketsAvailable = action { request =>
    val availableTickets = 1000
    val response = AvailabilityResponse("ok", Option(availableTickets))
    Ok(Json.toJson(response))
  }
}