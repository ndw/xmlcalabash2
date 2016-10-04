package com.xmlcalabash.graph

import akka.actor.{Actor, ActorRef}
import com.xmlcalabash.messages.{CloseMessage, ItemMessage, RanMessage, StartMessage}
import com.xmlcalabash.runtime.{Identity, StepController}

import scala.collection.mutable

/**
  * Created by ndw on 10/3/16.
  */
class NodeActor(node: Node) extends Actor {
  val openInputs = mutable.HashSet() ++ node.inputs()
  val dependsOn = mutable.HashSet() ++ node.dependsOn

  def checkRun(): Unit = {
    println("Check run: " + node.toString + " [" + openInputs.isEmpty.toString + ", " + dependsOn.isEmpty.toString + "]")
    if (openInputs.isEmpty && dependsOn.isEmpty) {
      println("RUN " + node.toString)
      node.run()
      context.stop(self)
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
    case _ => println(node.name.get + " says 'huh?'")
  }
}
