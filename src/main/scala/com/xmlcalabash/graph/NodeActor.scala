package com.xmlcalabash.graph

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import com.xmlcalabash.messages.{CloseMessage, ItemMessage, RanMessage, StartMessage}
import com.xmlcalabash.runtime.{Identity, StepController}

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
      node.run()
      log.debug("Node {} stops", node)
      context.stop(self)
    } else {
      log.debug("Node {} not ready to run (inputs ready: {}, dependencies ready: {})", node,
        openInputs.isEmpty, dependsOn.isEmpty)
    }
  }

  def receive = {
    case m: ItemMessage => node.receive(m.port, m)
    case m: CloseMessage =>
      openInputs.remove(m.port)
      checkRun()
    case m: StartMessage =>
      checkRun()
    case m: RanMessage =>
      if (dependsOn.contains(m.node)) {
        dependsOn.remove(m.node)
        checkRun()
      }
    case m: Any => log.debug("Node {} received unexpected message: {}", node, m)
  }
}
