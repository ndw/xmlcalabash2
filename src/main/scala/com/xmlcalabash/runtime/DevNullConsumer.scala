package com.xmlcalabash.runtime

import com.jafpl.messages.Message
import com.jafpl.steps.DataConsumer
import com.xmlcalabash.model.util.UniqueId

class DevNullConsumer extends DataConsumer {
  override def receive(port: String, message: Message): Unit = {
    // drop on the floor
  }
}
