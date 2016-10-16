package com.xmlcalabash.runtime

import com.jafpl.runtime.DefaultStep
import com.jafpl.messages.ItemMessage
import com.xmlcalabash.core.XProcEngine

/**
  * Created by ndw on 10/3/16.
  */
class Identity extends DefaultXProcStep {
  override def receive(port: String, msg: ItemMessage): Unit = {
    super.receive(port, msg)
    for (port <- outputPorts) {
      controller.send(port, msg.item)
    }
  }

  override def run(): Unit = {
    logger.info("RUNNING Identity: " + label)
  }

}
