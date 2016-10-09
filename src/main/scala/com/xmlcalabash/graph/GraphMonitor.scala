package com.xmlcalabash.graph

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive
import akka.event.Logging
import com.xmlcalabash.graph.GraphMonitor.{GFinish, GStart, GWatch}
import com.xmlcalabash.messages._

import scala.collection.mutable

/**
  * Created by ndw on 10/8/16.
  */

object GraphMonitor {
  case class GWatch(node: Node)
  case class GStart(node: Node)
  case class GFinish(node: Node)
}

class GraphMonitor(private val graph: Graph) extends Actor {
  val log = Logging(context.system, this)
  val watching = mutable.HashSet.empty[Node]

  def watch(node: Node): Unit = {
    log.debug("Watch " + node)
    watching += node
  }

  def start(node: Node): Unit = {
    watching += node
    log.debug("Start " + node + ": " + watching.size)
  }

  def finish(node: Node): Unit = {
    watching -= node
    log.debug("Finish " + node + ": " + watching.size)

    if (watching.isEmpty) {
      graph.finish()
      context.system.terminate()
    }
  }

  final def receive = {
    case GWatch(node) => watch(node)
    case GStart(node) => start(node)
    case GFinish(node) => finish(node)
    case m: Any => log.debug("Unexpected message: {}", m)
  }
}
