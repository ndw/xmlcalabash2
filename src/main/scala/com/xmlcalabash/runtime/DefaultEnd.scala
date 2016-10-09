package com.xmlcalabash.runtime

import akka.actor
import com.xmlcalabash.messages.ItemMessage

/**
  * Created by ndw on 10/3/16.
  */
class DefaultEnd(start: CompoundStart) extends DefaultStep("end_" + start.toString) with CompoundEnd {
  override def receive(port: String, msg: ItemMessage): Unit = {
    super.receive(port, msg)
    start.receiveResult(port, msg)
  }
  override def receiveResult(port: String, msg: ItemMessage) = {
    logger.debug("{} receiveResult on {}: {}", this, port, msg)
    controller.send(port.substring(2), msg.item)
  }

  override def run(): Unit = {
    super.run()
    logger.info("RAN COMPOUND END")
    if (start.completed) {
      controller.stop()
    }
  }

}
