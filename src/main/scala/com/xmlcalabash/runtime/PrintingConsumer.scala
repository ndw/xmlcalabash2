package com.xmlcalabash.runtime

import java.io.{ByteArrayOutputStream, File, FileOutputStream, PrintStream}

import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.steps.DataConsumer
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.model.util.{MDUtils, UniqueId, XProcConstants}
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.{Serializer, XdmValue}

class PrintingConsumer(config: XMLCalabash, outputs: Option[List[String]]) extends DataConsumer {
  private val _id = UniqueId.nextId.toString
  private var index = 0

  def this(config: XMLCalabash) = {
    this(config, None)
  }

  def this(config: XMLCalabash, outputs: List[String]) = {
    this(config, Some(outputs))
  }

  override def id: String = _id

  override def receive(port: String, message: Message): Unit = {
    message match {
      case item: ItemMessage =>
        if (outputs.isEmpty || (index >= outputs.get.length)) {
          if (MDUtils.textContentType(item.metadata)) {
            println(item.item)
          } else if (MDUtils.xmlContentType(item.metadata) || MDUtils.jsonContentType(item.metadata)) {
            val stream = new ByteArrayOutputStream()
            val serializer = config.processor.newSerializer(stream)
            serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes")
            item.item match {
              case value: XdmValue => S9Api.serialize(config, value, serializer)
            }
            println(stream.toString("UTF-8"))
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
