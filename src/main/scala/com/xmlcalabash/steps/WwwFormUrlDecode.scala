package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{XdmAtomicValue, XdmMap, XdmValue}

import java.net.URLDecoder
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.IteratorHasAsJava

class WwwFormUrlDecode() extends DefaultXmlStep {
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.JSONRESULT

  override def run(context: StaticContext): Unit = {
    val value = stringBinding(XProcConstants._value).trim
    val encoding = optionalStringBinding(XProcConstants.cx_encoding).getOrElse("UTF-8")

    // First decode the string
    val decoded = mutable.HashMap.empty[String, ListBuffer[XdmAtomicValue]]
    // Do this the hard way because .split() doesn't do the right thing
    var pos = 0
    var part = ""
    var parts = value
    while (parts != "") {
      pos = parts.indexOf("&")
      if (pos >= 0) {
        part = parts.substring(0, pos)
        parts = parts.substring(pos + 1).trim
        if (parts == "") {
          // trailing & are not allowed
          throw XProcException.xcValueNotFormUrlEncoded(value, location)
        }
      } else {
        part = parts
        parts = ""
      }

      pos = part.indexOf("=")
      if (pos > 0) {
        val keyname = URLDecoder.decode(part.substring(0, pos), encoding)
        val keyvalue = URLDecoder.decode(part.substring(pos + 1), encoding)

        if (!decoded.contains(keyname)) {
          decoded.put(keyname, ListBuffer.empty[XdmAtomicValue])
        }

        val vlist = decoded(keyname)
        vlist += new XdmAtomicValue(keyvalue)
      } else {
        throw XProcException.xcValueNotFormUrlEncoded(value, location)
      }
    }

    var json = new XdmMap()
    for ((key, value) <- decoded) {
      if (value.size == 1) {
        json = json.put(new XdmAtomicValue(key), value.head)
      } else {
        json = json.put(new XdmAtomicValue(key), new XdmValue(value.iterator.asJava))
      }
    }

    consumer.get.receive("result", json, new XProcMetadata(MediaType.JSON))
  }
}
