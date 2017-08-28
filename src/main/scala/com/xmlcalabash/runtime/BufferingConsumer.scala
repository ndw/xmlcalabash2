package com.xmlcalabash.runtime

import com.jafpl.messages.Metadata
import com.jafpl.steps.DataConsumer

import scala.collection.mutable.ListBuffer

class BufferingConsumer extends DataConsumer {
  private val _items = ListBuffer.empty[Any]

  def items: List[Any] = _items.toList

  override def receive(port: String, item: Any, metadata: Metadata): Unit = {
    _items += item
  }
}
