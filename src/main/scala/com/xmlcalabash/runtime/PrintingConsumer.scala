package com.xmlcalabash.runtime

import java.io.{ByteArrayOutputStream, File, FileOutputStream, InputStream, PrintStream}
import java.util.Base64

import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.steps.DataConsumer
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.UniqueId
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

        val pos = if (outputs.isEmpty || (index >= outputs.get.length)) {
          System.out
        } else {
          val file = new File(outputs.get(index))
          index += 1

          val fos = new FileOutputStream(file)
          new PrintStream(fos)
        }

        item.item match {
          case is: InputStream =>
            val stream = new ByteArrayOutputStream()
            val buf = Array.fill[Byte](4096)(0)
            var len = is.read(buf, 0, buf.length)
            while (len >= 0) {
              stream.write(buf, 0, len)
              len = is.read(buf, 0, buf.length)
            }
            pos.write(stream.toByteArray)
          case value: XdmValue =>
            val stream = new ByteArrayOutputStream()
            val serializer = config.processor.newSerializer(stream)

            if (ctype.jsonContentType) {
              serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes")
            } else {
              serialization.setOutputProperties(serializer)
            }

            S9Api.serialize(config.config, value, serializer)
            pos.print(stream.toString("UTF-8"))
          case _ =>
            throw new RuntimeException(s"Don't know how to print ${item.item}")
        }
    }
  }
}
