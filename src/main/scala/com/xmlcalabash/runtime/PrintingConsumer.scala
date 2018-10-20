package com.xmlcalabash.runtime

import java.io.{ByteArrayOutputStream, File, FileOutputStream, InputStream, PrintStream}

import com.jafpl.messages.Message
import com.jafpl.steps.DataConsumer
import com.xmlcalabash.messages.{AnyItemMessage, XProcItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.UniqueId
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.{QName, Serializer}

class PrintingConsumer private(config: XMLCalabashRuntime, serialOpts: Map[QName,String], outputs: Option[List[String]]) extends DataConsumer {
  private val _id = UniqueId.nextId.toString
  private var index = 0

  def this(config: XMLCalabashRuntime, serialization: Map[QName,String]) = {
    this(config, serialization, None)
  }

  def this(config: XMLCalabashRuntime, serialization: Map[QName,String], outputs: List[String]) = {
    this(config, serialization, Some(outputs))
  }

  override def id: String = _id

  override def receive(port: String, message: Message): Unit = {
    message match {
      case msg: XProcItemMessage =>
        val ctype = msg.metadata.contentType

        val pos = if (outputs.isEmpty || (index >= outputs.get.length)) {
          System.out
        } else {
          val file = new File(outputs.get(index))
          index += 1

          val fos = new FileOutputStream(file)
          new PrintStream(fos)
        }

        message match {
          case msg: AnyItemMessage =>
            msg.shadow match {
              case is: InputStream =>
                val stream = new ByteArrayOutputStream()
                val buf = Array.fill[Byte](4096)(0)
                var len = is.read(buf, 0, buf.length)
                while (len >= 0) {
                  stream.write(buf, 0, len)
                  len = is.read(buf, 0, buf.length)
                }
                pos.write(stream.toByteArray)
              case bytes: Array[Byte] =>
                pos.write(bytes)
              case _ =>
                throw new RuntimeException(s"Don't know how to serialize item: ${msg.shadow}")
            }
          case msg: XdmValueItemMessage =>
            val stream = new ByteArrayOutputStream()
            val serializer = config.processor.newSerializer(stream)

            S9Api.configureSerializer(serializer, config.defaultSerializationOptions(ctype.toString))
            S9Api.configureSerializer(serializer, serialOpts)

            if (!ctype.xmlContentType) {
              serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes")
            }

            S9Api.serialize(config.config, msg.item, serializer)
            pos.print(stream.toString("UTF-8"))
          case _ =>
            throw new RuntimeException(s"Don't know how to print ${msg.item}")
        }
    }
  }
}
