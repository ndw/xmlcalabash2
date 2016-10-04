package com.xmlcalabash.graph

import com.xmlcalabash.messages.ItemMessage
import com.xmlcalabash.runtime.{Step, StepController}

/**
  * Created by ndw on 10/3/16.
  */
class Fan(name: String) extends Step {
  private var controller: StepController = _
  private var inputPorts: collection.Set[String] = _
  private var outputPorts: collection.Set[String] = _

  override def init(controller: StepController,
                    inputPorts: collection.Set[String],
                    outputPorts: collection.Set[String],
                    options: collection.Set[String]): Boolean = {
    this.controller = controller
    this.inputPorts = inputPorts
    this.outputPorts = outputPorts
    true
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    for (port <- outputPorts) {
      controller.send(port, msg.item)
    }
  }

  override def run(): Unit = {
    // nothing to do
  }
}
