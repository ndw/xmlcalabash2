package com.xmlcalabash.runtime

import com.jafpl.messages.Message
import com.jafpl.steps.DataConsumer
import com.xmlcalabash.model.util.UniqueId

class DevNullConsumer extends DataConsumer {
  private val _id = UniqueId.nextId.toString
  override def id: String = _id
  override def receive(port: String, message: Message): Unit = {
    // drop on the floor
  }
}
