package com.xmlcalabash.config

import com.jafpl.messages.ItemMessage
import com.jafpl.steps.DataConsumer

trait DeliveryAgent {
  def deliver(message: ItemMessage, consumer: DataConsumer, port: String): Unit
}
