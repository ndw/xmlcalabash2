package com.xmlcalabash.runtime

import com.jafpl.messages.Message
import com.jafpl.steps.DataConsumer
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XProcItemMessage

import scala.collection.mutable.ListBuffer

class BufferingConsumer extends DataConsumer {
  private val _items = ListBuffer.empty[XProcItemMessage]

  def messages: List[XProcItemMessage] = _items.toList

  override def consume(port: String, message: Message): Unit = {
    message match {
      case msg: XProcItemMessage =>
        _items += msg
      case _ =>
        throw XProcException.xiInvalidMessage(None, message)
    }
  }
}
