package controllers

import javax.inject.Inject

import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter

import scala.concurrent.Future

class Application @Inject()(action: BaseAction) extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def jsRoutes = action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.Events.list,
        routes.javascript.Events.ticketBlocksForEvent,
        routes.javascript.Orders.create
      )
    )
  }

}


class BaseAction extends ActionBuilder[Request] {
  override def invokeBlock[A](request: Request[A],
                              block: Request[A] => Future[Result]) = {
    block(request)
  }
}