package com.xmlcalabash.graph

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import com.xmlcalabash.graph.GraphMonitor.{GFinish, GFinished, GStart}
import com.xmlcalabash.messages._
import com.xmlcalabash.runtime.{CompoundStart, Identity, StepController}

import scala.collection.mutable

/**
  * Created by ndw on 10/3/16.
  */
private[graph] class NodeActor(node: Node) extends Actor {
  val log = Logging(context.system, this)
  val openInputs = mutable.HashSet() ++ node.inputs()
  val dependsOn = mutable.HashSet() ++ node.dependsOn

  def checkRun(): Unit = {
    if (openInputs.isEmpty && dependsOn.isEmpty) {
      run()
    } else {
      log.debug("Node {} not ready to run (inputs ready: {}, dependencies ready: {})", node,
        openInputs.isEmpty, dependsOn.isEmpty)
    }
  }

  private def run() = {
    log.debug("RUN   " + node)
    node.graph.monitor ! GStart(node)
    node.run()

    node match {
      case n: LoopStart =>
        if (n.stepFinished) {
          node.graph.monitor ! GFinish(node)
        }
      case n: LoopEnd =>
        Unit
      case _ => node.graph.monitor ! GFinish(node)
    }
  }

  def receive = {
    case m: ItemMessage =>
      log.debug("MSG   {}", node)
      node.receive(m.port, m)
    case m: CloseMessage =>
      log.debug("CLOSE {}: {}", m.port, node)
      openInputs.remove(m.port)
      checkRun()
    case m: StartMessage =>
      log.debug("SMSG  {}", node)
      checkRun()
    case m: RanMessage =>
      log.debug("RAN  {}", node)
      if (dependsOn.contains(m.node)) {
        dependsOn.remove(m.node)
        checkRun()
      }
    case m: ResetMessage =>
      log.debug("RESET {}", node)
      node.reset()
    case m: GFinished =>
      node match {
        case ls: LoopStart =>
          log.debug("RTORS {}", ls)
          ls.readyToRestart()
        case _ => log.debug("Node {} didn't expect to be notified of subgraph completion")
      }
    case m: Any => log.debug("Node {} received unexpected message: {}", node, m)
  }
}
