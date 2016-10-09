package com.xmlcalabash.graph

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive
import akka.event.Logging
import com.xmlcalabash.graph.GraphMonitor._
import com.xmlcalabash.messages._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/8/16.
  */

object GraphMonitor {
  case class GWatch(node: Node)
  case class GStart(node: Node)
  case class GFinish(node: Node)
  case class GSubgraph(ref: ActorRef, subpipeline: List[Node])
  case class GFinished()
}

class GraphMonitor(private val graph: Graph) extends Actor {
  val log = Logging(context.system, this)
  val watching = mutable.HashSet.empty[Node]
  val subgraphs = mutable.HashMap.empty[ActorRef, List[Node]]

  def watch(node: Node): Unit = {
    log.debug("WATCH " + node)
    watching += node
  }

  def subgraph(ref: ActorRef, subpipeline: List[Node]): Unit = {
    subgraphs.put(ref, subpipeline)
  }

  def start(node: Node): Unit = {
    watching += node
    log.debug("START " + watching.size + ": " + subgraphs.size + ": " + node)
  }

  def finish(node: Node): Unit = {
    val runningNow = ListBuffer.empty[ActorRef]
    for (ref <- subgraphs.keySet) {
      var running = false
      for (node <- subgraphs(ref)) {
        running = running || watching.contains(node)
      }
      if (running) {
        runningNow += ref
      }
    }

    watching -= node
    log.debug("FINIS " + watching.size + ": " + runningNow.size + ": " + node)

    for (ref <- runningNow) {
      var running = false
      for (node <- subgraphs(ref)) {
        running = running || watching.contains(node)
      }
      if (!running) {
        log.debug("TELL   " + watching.size + ": " + runningNow.size + ": " + ref)
        ref ! GFinished()
      } else {
        log.debug("NOTELL " + watching.size + ": " + runningNow.size + ": " + ref)
      }
    }

    if (watching.isEmpty) {
      log.debug("Pipeline execution complete")
      graph.finish()
      context.system.terminate()
    }
  }

  final def receive = {
    case GWatch(node) => watch(node)
    case GStart(node) => start(node)
    case GFinish(node) => finish(node)
    case GSubgraph(ref, subpipline) => subgraph(ref, subpipline)
    case m: Any => log.debug("Unexpected message: {}", m)
  }
}
