package controllers

import javax.inject.{Inject, Named}

import akka.actor.ActorRef
import play.api.mvc._
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import akka.util.Timeout
import com.semisafe.ticketoverlords.{Event, TicketBlock, TicketLordsExecutionContext}
import controllers.responses._
import play.api.Configuration

class Events @Inject()(config: Configuration,
                       events: com.semisafe.ticketoverlords.Events,
                       ticketBlocks: com.semisafe.ticketoverlords.TicketBlocks,
                       @Named("ticketIssuer") ticketIssuer: ActorRef,
                       ticketLordsExecutionContext: TicketLordsExecutionContext,
                       action: BaseAction)
                      (implicit ec: ExecutionContext) extends Controller {
  def list = Action.async { request =>
    val eventFuture: Future[Seq[Event]] = events.list

    val response = eventFuture.map { events =>
      Ok(Json.toJson(SuccessResponse(events)))
    }

    response
  }

  def getByID(eventID: Long) = action.async { request =>
    val eventFuture: Future[Option[Event]] = events.getByID(eventID)

    eventFuture.map { event =>
      event.fold {
        NotFound(Json.toJson(ErrorResponse(NOT_FOUND, "No event found")))
      } { e =>
        Ok(Json.toJson(SuccessResponse(e)))
      }
    }
  }

  def create = action.async(parse.json) { request =>
    val incomingBody = request.body.validate[Event]

    incomingBody.fold(error => {
      val errorMessage = s"Invalid JSON: ${error}"
      val response = ErrorResponse(ErrorResponse.INVALID_JSON, errorMessage)
      Future.successful(BadRequest(Json.toJson(response)))
    }, { event =>
      // save event and get a copy back
      val createdEventFuture: Future[Event] = events.create(event)

      createdEventFuture.map { createdEvent =>
        Created(Json.toJson(SuccessResponse(createdEvent)))
      }

    })
  }

  def ticketBlocksForEvent(eventID: Long) = action.async { request =>
    val eventFuture = events.getByID(eventID)

    eventFuture.flatMap { event =>
      event.fold {
        Future.successful(
          NotFound(Json.toJson(ErrorResponse(NOT_FOUND, "No event found"))))
      } { e =>
        val timeoutKey = "ticketoverlords.timeouts.ticket_availability_ms"
        val configuredTimeout = config.getInt(timeoutKey)
        val resolvedTimeout = configuredTimeout.getOrElse(400)
        implicit val timeout = Timeout(resolvedTimeout.milliseconds)

        val blocks: Future[Seq[TicketBlock]] = e.ticketBlocksWithAvailability(ticketBlocks, ticketIssuer)(timeout, ticketLordsExecutionContext)
        blocks.map { tb =>
          Ok(Json.toJson(SuccessResponse(tb)))
        }
      }
    }
  }
}

