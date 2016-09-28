package controllers

import javax.inject.Inject

import play.api.mvc._
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import com.semisafe.ticketoverlords.TicketBlock
import controllers.responses._
import play.api.Configuration

class TicketBlocks @Inject()(config: Configuration,
                             ticketBlocks: com.semisafe.ticketoverlords.TicketBlocks,
                             action: BaseAction)
                             (implicit ec: ExecutionContext) extends Controller {
  def list = action.async { request =>
    val blocks: Future[Seq[TicketBlock]] = ticketBlocks.list

    blocks.map { tbs =>
      Ok(Json.toJson(SuccessResponse(tbs)))
    }
  }

  def getByID(ticketBlockID: Long) = action.async { request =>
    val ticketBlockFuture: Future[Option[TicketBlock]] = ticketBlocks.getByID(ticketBlockID)

    ticketBlockFuture.map { ticketBlock =>
      ticketBlock.fold {
        NotFound(Json.toJson(ErrorResponse(NOT_FOUND, "No ticket block found")))
      } { tb =>
        Ok(Json.toJson(SuccessResponse(tb)))
      }
    }
  }

  def create = action.async(parse.json) { request =>
    val incomingBody = request.body.validate[TicketBlock]

    incomingBody.fold(error => {
      val errorMessage = s"Invalid JSON: ${error}"
      val response = ErrorResponse(ErrorResponse.INVALID_JSON, errorMessage)
      Future.successful(BadRequest(Json.toJson(response)))
    }, { ticketBlock =>
      // save ticket block and get a copy back
      val createdBlock: Future[TicketBlock] = ticketBlocks.create(ticketBlock)

      createdBlock.map { cb =>
        Created(Json.toJson(SuccessResponse(cb)))
      }
    })
  }
}
