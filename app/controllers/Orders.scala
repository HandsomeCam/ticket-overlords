package controllers

import javax.inject.{Inject, Named}

import akka.actor.ActorRef
import play.api.mvc._
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import com.semisafe.ticketoverlords._
import controllers.responses._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import play.api.{Configuration, Logger}
import play.filters.csrf._

class Orders @Inject()(config: Configuration,
                       @Named("ticketIssuer") issuer: ActorRef,
                       orders: com.semisafe.ticketoverlords.Orders,
                       CSRFCheck: CSRFCheck,
                       action: BaseAction)
                       (implicit ec: ExecutionContext) extends Controller {

  def list = action.async { request =>
    val list = orders.list
    list.map { o =>
      Ok(Json.toJson(SuccessResponse(o)))
    }
  }

  def getByID(orderID: Long) = action.async { request =>
    val orderFuture = orders.getByID(orderID)

    orderFuture.map { order =>
      order.fold {
        NotFound(Json.toJson(ErrorResponse(NOT_FOUND, "No order found")))
      } { o =>
        Ok(Json.toJson(SuccessResponse(o)))
      }
    }
  }

  def create = CSRFCheck {
    action.async(parse.json) { request =>
      val incomingBody = request.body.validate[Order]

      incomingBody.fold(error => {
        val errorMessage = s"Invalid JSON: ${error}"
        val response = ErrorResponse(ErrorResponse.INVALID_JSON, errorMessage)
        Future.successful(BadRequest(Json.toJson(response)))
      }, { order =>
        val timeoutKey = "ticketoverlords.timeouts.issuer"
        val configuredTimeout = config.getInt(timeoutKey)
        val resolvedTimeout = configuredTimeout.getOrElse(5)
        implicit val timeout = Timeout(resolvedTimeout.seconds)

        val orderFuture = (issuer ? order).mapTo[Order]

        // Convert successful future to Json
        orderFuture.map { createdOrder =>
          Ok(Json.toJson(SuccessResponse(createdOrder)))
        }.recover({
          case ita: InsufficientTicketsAvailable => {
            val responseMessage =
              "There are not enough tickets remaining to complete this order." +
                s" Quantity Remaining: ${ita.ticketsAvailable}"

            val response = ErrorResponse(
              ErrorResponse.NOT_ENOUGH_TICKETS,
              responseMessage)

            BadRequest(Json.toJson(response))
          }
          case tba: TicketBlockUnavailable => {
            val responseMessage =
              s"Ticket Block ${order.ticketBlockID} is not available."
            val response = ErrorResponse(
              ErrorResponse.TICKET_BLOCK_UNAVAILABLE,
              responseMessage)

            BadRequest(Json.toJson(response))
          }
          case unexpected => {
            Logger.error(
              s"Unexpected error while placing an order: ${unexpected.toString}")
            val response = ErrorResponse(
              INTERNAL_SERVER_ERROR,
              "An unexpected error occurred")

            InternalServerError(Json.toJson(response))
          }
        })
      })
    }
  }
}
