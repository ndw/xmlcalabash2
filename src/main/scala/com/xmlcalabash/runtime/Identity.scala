package com.xmlcalabash.runtime

import com.xmlcalabash.messages.ItemMessage
import net.sf.saxon.s9api.QName

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
