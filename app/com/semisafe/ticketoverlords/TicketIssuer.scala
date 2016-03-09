package com.semisafe.ticketoverlords

import javax.inject.Inject

import akka.actor.Actor
import akka.actor.Status.{Failure => ActorFailure}
import play.api.libs.concurrent.Execution.Implicits._

import akka.actor.{ActorRef, ActorSystem, Props}

case class AvailabilityCheck(ticketBlockID: Long)

case class InsufficientTicketsAvailable(
  ticketBlockID: Long,
  ticketsAvailable: Int) extends Throwable

case class TicketBlockUnavailable(
  ticketBlockID: Long) extends Throwable

case class TicketBlockCreated(ticketBlock: TicketBlock)


class TicketIssuer @Inject()(ticketBlockDao: TicketBlockDao) extends Actor {

  var workers = Map[Long, ActorRef]()

  def createWorker(ticketBlockID: Long) {
    if (!workers.contains(ticketBlockID)) {
      val worker = context.actorOf(
        Props(classOf[TicketIssuerWorker], ticketBlockID),
        name = ticketBlockID.toString)
      workers = workers + (ticketBlockID -> worker)
    }
  }

  override def preStart = {
    val ticketBlocksResult = ticketBlockDao.list

    for {
      ticketBlocks <- ticketBlocksResult
      block <- ticketBlocks
      id <- block.id
    } createWorker(id)
  }

  def placeOrder(order: Order) {
    val workerRef = workers.get(order.ticketBlockID)

    workerRef.fold {
      sender ! ActorFailure(TicketBlockUnavailable(order.ticketBlockID))
    } { worker =>
      worker forward order
    }
  }

  def receive = {
    case order: Order          => placeOrder(order)
    case a: AvailabilityCheck  => checkAvailability(a)
    case TicketBlockCreated(t) => t.id.foreach(createWorker)
  }

  def checkAvailability(message: AvailabilityCheck) = {
    val workerRef = workers.get(message.ticketBlockID)

    workerRef.fold {
      sender ! ActorFailure(TicketBlockUnavailable(message.ticketBlockID))
    } { worker =>
      worker forward message
    }
  }
}

object TicketIssuer {
  def getSelection(implicit system:ActorSystem) = system.actorSelection("/user/ticketIssuer")
}
