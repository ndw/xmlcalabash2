package com.xmlcalabash.runtime

import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.steps.DataConsumer

class PrintingConsumer extends DataConsumer {
  override def receive(port: String, message: Message): Unit = {
    message match {
      case item: ItemMessage =>
        println(item.item)
      case _ => println(message)
    }
  }
}
