import com.google.inject.AbstractModule
import com.semisafe.ticketoverlords.{TicketIssuer, TicketLordsExecutionContext}
import play.api.libs.concurrent.AkkaGuiceSupport

class Module extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {

    bindActor[TicketIssuer]("ticketIssuer")
    bind(classOf[TicketLordsExecutionContext]).asEagerSingleton()
  }
}
