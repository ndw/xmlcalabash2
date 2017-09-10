package com.xmlcalabash.config

import com.jafpl.messages.Message
import com.jafpl.steps.DataConsumer

trait DeliveryAgent {
  def deliver(message: Message, consumer: DataConsumer, port: String): Unit
}
