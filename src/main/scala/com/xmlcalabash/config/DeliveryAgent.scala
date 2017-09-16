package com.xmlcalabash.config

import com.jafpl.messages.Message
import com.jafpl.steps.DataConsumer

trait DeliveryAgent {
  def deliver(from: Any, fromPort: String, message: Message, consumer: DataConsumer, port: String): Unit
}
