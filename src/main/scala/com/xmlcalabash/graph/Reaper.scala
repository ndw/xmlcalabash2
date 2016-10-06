package com.xmlcalabash.graph

import akka.actor.{Actor, ActorRef, Terminated}
import akka.event.Logging
import com.xmlcalabash.graph.Reaper.WatchMe

import scala.collection.mutable.ArrayBuffer

object Reaper {
  // Used by others to register an Actor for watching
  case class WatchMe(ref: ActorRef)
}

abstract class Reaper extends Actor {
  val log = Logging(context.system, this)

  // Keep track of what we're watching
  val watched = ArrayBuffer.empty[ActorRef]

  // Derivations need to implement this method.  It's the
  // hook that's called when everything's dead
  def allSoulsReaped(): Unit

  // Watch and check for termination
  final def receive = {
    case WatchMe(ref) =>
      log.debug("Reaper watches {}", ref)
      context.watch(ref)
      watched += ref
    case Terminated(ref) =>
      log.info("Reaper sees termination {}", ref)
      watched -= ref
      if (watched.isEmpty) {
        allSoulsReaped()
      }
  }
}

private[graph] class ProductionReaper(val graph: Graph) extends Reaper {
  def allSoulsReaped(): Unit = {
    log.info("All steps have finished")
    graph.finish()
    context.system.terminate()
  }
}
