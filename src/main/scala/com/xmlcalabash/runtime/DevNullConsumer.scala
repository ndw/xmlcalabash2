package com.xmlcalabash.runtime

import com.jafpl.messages.Metadata
import com.jafpl.steps.DataConsumer

import scala.collection.mutable.ListBuffer

class DevNullConsumer extends DataConsumer {
  override def receive(port: String, item: Any, metadata: Metadata): Unit = {
    // drop on the floor
  }
}
