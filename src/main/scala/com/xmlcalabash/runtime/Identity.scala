package com.xmlcalabash.runtime

import com.jafpl.runtime.DefaultStep
import com.jafpl.messages.ItemMessage

/**
  * Created by ndw on 10/3/16.
  */
class Identity(name: String) extends DefaultStep(name) {
  override def receive(port: String, msg: ItemMessage): Unit = {
    super.receive(port, msg)
    for (port <- outputPorts) {
      controller.send(port, msg.item)
    }
  }
}
