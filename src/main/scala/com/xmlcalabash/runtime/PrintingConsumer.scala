package com.xmlcalabash.runtime

import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.steps.DataConsumer
import com.xmlcalabash.model.util.UniqueId

class PrintingConsumer extends DataConsumer {
  private val _id = UniqueId.nextId.toString
  override def id: String = _id
  override def receive(port: String, message: Message): Unit = {
    message match {
      case item: ItemMessage =>
        println(item.item)
      case _ => println(message)
    }
  }
}
