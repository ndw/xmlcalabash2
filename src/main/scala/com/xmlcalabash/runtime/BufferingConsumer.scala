package com.xmlcalabash.runtime

import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.steps.DataConsumer
import com.xmlcalabash.model.util.UniqueId

import scala.collection.mutable.ListBuffer

class BufferingConsumer extends DataConsumer {
  private val _id = UniqueId.nextId.toString
  private val _items = ListBuffer.empty[Any]

  def items: List[Any] = _items.toList

  override def id: String = _id
  override def receive(port: String, message: Message): Unit = {
    message match {
      case item: ItemMessage =>
        _items += item.item
      case _ => Unit
    }
  }
}
