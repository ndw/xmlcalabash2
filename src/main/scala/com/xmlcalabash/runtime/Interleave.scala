package com.xmlcalabash.runtime

import com.xmlcalabash.items.GenericItem
import com.xmlcalabash.messages.ItemMessage

/**
  * Created by ndw on 10/3/16.
  */
class Interleave(name: String) extends Step {
  private var controller: StepController = _
  private var inputPorts: collection.Set[String] = _
  private var outputPorts: collection.Set[String] = _

  private val leftSource = collection.mutable.ListBuffer.empty[GenericItem]
  private val rightSource = collection.mutable.ListBuffer.empty[GenericItem]

  override def init(controller: StepController,
                    inputPorts: collection.Set[String],
                    outputPorts: collection.Set[String],
                    options: collection.Set[String]): Boolean = {
    this.controller = controller
    this.inputPorts = inputPorts
    this.outputPorts = outputPorts

    inputPorts.size == 2 && inputPorts.contains("left") && inputPorts.contains("right")
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    println("Step " + name + " received msg " + msg.sequenceNo + " on " + port + " from " + msg.senderId + ": " + msg.item)
    if (port == "left") {
      leftSource.append(msg.item)
    } else {
      rightSource.append(msg.item)
    }
  }

  override def run(): Unit = {
    while (leftSource.nonEmpty || rightSource.nonEmpty) {
      if (leftSource.nonEmpty) {
        controller.send("result", leftSource.remove(0))
      }
      if (rightSource.nonEmpty) {
        controller.send("result", rightSource.remove(0))
      }
    }
  }
}
