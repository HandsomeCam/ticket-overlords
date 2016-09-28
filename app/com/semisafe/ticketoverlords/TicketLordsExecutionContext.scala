package com.semisafe.ticketoverlords

import javax.inject.Inject

import scala.concurrent.ExecutionContextExecutor
import akka.actor.ActorSystem
import akka.dispatch.MessageDispatcher

class TicketLordsExecutionContext @Inject()(system: ActorSystem)
  extends CustomExecutionContext(system, "ticketlords-dispatcher")

abstract class CustomExecutionContext(system: ActorSystem, name: String) extends ExecutionContextExecutor {
  private val dispatcher: MessageDispatcher = system.dispatchers.lookup(name)

  override def execute(command: Runnable) = dispatcher.execute(command)

  override def reportFailure(cause: Throwable) = dispatcher.reportFailure(cause)
}