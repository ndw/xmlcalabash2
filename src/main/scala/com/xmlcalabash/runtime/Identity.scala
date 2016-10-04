package com.xmlcalabash.runtime

import com.xmlcalabash.messages.ItemMessage

/**
  * Created by ndw on 10/3/16.
  */
class Identity(name: String) extends Step {
  private var controller: StepController = _
  private var inputPorts: collection.Set[String] = _
  private var outputPorts: collection.Set[String] = _
  private var options: collection.Set[String] = _
  private var count = 0

  override def init(controller: StepController,
                    inputPorts: collection.Set[String],
                    outputPorts: collection.Set[String],
                    options: collection.Set[String]): Boolean = {
    this.controller = controller
    this.inputPorts = inputPorts
    this.outputPorts = outputPorts
    this.options = options

    true
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    println("Step " + name + " received msg " + msg.sequenceNo + " on " + port + " from " + msg.senderId + ": " + msg.item)
    count += 1
    for (port <- outputPorts) {
      controller.send(port, msg.item)
    }
  }

  override def run(): Unit = {
    println("Step " + name + " ran: " + count)
    Thread.sleep(250)
  }
}
