package com.xmlcalabash.runtime

import com.jafpl.steps.DataProvider

import scala.collection.mutable.ListBuffer

class BufferingConsumer extends DataProvider {
  private val _items = ListBuffer.empty[Any]

  def items: List[Any] = _items.toList

  override def send(item: Any): Unit = {
    _items += item
  }

  override def close(): Unit = Unit
}
