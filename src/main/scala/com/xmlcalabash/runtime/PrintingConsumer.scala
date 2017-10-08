package com.xmlcalabash.runtime

import java.io.{File, FileOutputStream, PrintStream}

import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.steps.DataConsumer
import com.xmlcalabash.model.util.{MDUtils, UniqueId, XProcConstants}

class PrintingConsumer (outputs: Option[List[String]]) extends DataConsumer {
  private val _id = UniqueId.nextId.toString
  private var index = 0

  def this() = {
    this(None)
  }

  def this(outputs: List[String]) = {
    this(Some(outputs))
  }

  override def id: String = _id

  override def receive(port: String, message: Message): Unit = {
    message match {
      case item: ItemMessage =>
        if (outputs.isEmpty || (index >= outputs.get.length)) {
          if (MDUtils.textContentType(item.metadata)) {
            println(item.item)
          } else if (MDUtils.xmlContentType(item.metadata)) {
            println(item.item)
          } else {
            println(s"Eliding ${MDUtils.contentType(item.metadata)} content")
          }
        } else {
          val file = new File(outputs.get(index))
          index += 1

          val fos = new FileOutputStream(file)
          val pos = new PrintStream(fos)
          if (MDUtils.textContentType(item.metadata)) {
            pos.print(item.item)
          } else if (MDUtils.xmlContentType(item.metadata)) {
            pos.print(item.item)
          } else {
            val bytes: Array[Byte] = item.item.asInstanceOf[Array[Byte]]
            pos.write(bytes)
          }

          pos.close()
          fos.close()
        }
      case _ =>
        throw new RuntimeException("Unexpected message type: " + message)
    }
  }
}
