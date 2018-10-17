package com.xmlcalabash.runtime

import java.io.{ByteArrayOutputStream, File, FileOutputStream, PrintStream}

import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.steps.DataConsumer
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{UniqueId, XProcConstants}
import com.xmlcalabash.util.{MediaType, S9Api, SerializationOptions}
import net.sf.saxon.s9api.{Serializer, XdmValue}

class PrintingConsumer private(config: XMLCalabashRuntime, serialization: SerializationOptions, outputs: Option[List[String]]) extends DataConsumer {
  private val _id = UniqueId.nextId.toString
  private var index = 0

  def this(config: XMLCalabashRuntime, serialization: SerializationOptions) = {
    this(config, serialization, None)
  }

  def this(config: XMLCalabashRuntime, serialization: SerializationOptions, outputs: List[String]) = {
    this(config, serialization, Some(outputs))
  }

  override def id: String = _id

  override def receive(port: String, message: Message): Unit = {
    message match {
      case item: ItemMessage =>
        val ctype = item.metadata match {
          case meta: XProcMetadata => meta.contentType
          case _ => MediaType.OCTET_STREAM
        }

        if (outputs.isEmpty || (index >= outputs.get.length)) {
          if (ctype.markupContentType) {
            val stream = new ByteArrayOutputStream()
            val serializer = config.processor.newSerializer(stream)

            if (ctype.jsonContentType) {
              serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes")
            } else {
              serialization.setOutputProperties(serializer)
            }

            item.item match {
              case value: XdmValue => S9Api.serialize(config.config, value, serializer)
            }
            println(stream.toString("UTF-8"))
          } else if (ctype.textContentType) {
            println(item.item)
          } else {
            println(s"Eliding $ctype content")
          }
        } else {
          val file = new File(outputs.get(index))
          index += 1

          val fos = new FileOutputStream(file)
          val pos = new PrintStream(fos)
          if (ctype.textContentType) {
            pos.print(item.item)
          } else if (ctype.xmlContentType) {
            pos.print(item.item)
          } else {
            val bytes: Array[Byte] = item.item.asInstanceOf[Array[Byte]]
            pos.write(bytes)
          }

          pos.close()
          fos.close()
        }
      case _ =>
        throw XProcException.xiBadMessage(message, None)
    }
  }
}
