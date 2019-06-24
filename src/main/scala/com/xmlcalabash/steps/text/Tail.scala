package com.xmlcalabash.steps.text

import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmAtomicValue}

import scala.collection.mutable.ListBuffer

class Tail() extends TextLines {
  private val _count = new QName("", "count")

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.TEXTSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.TEXTRESULT

  override def run(context: StaticContext): Unit = {
    // There must be a better way to do this!
    val count = bindings(_count).value.toString.toInt
    var newLines = ListBuffer.empty[String]

    if (count == 0) {
      newLines = lines
    } else if (count > 0) {
      if (count < lines.size) {
        newLines = lines.drop(lines.size - count)
      } else {
        newLines = lines
      }
    } else if (count < 0) {
      newLines = lines.dropRight(-count)
    }

    var hlines = ""
    for (line <- newLines) {
      if (hlines != "") {
        hlines += "\n"
      }
      hlines += line
    }

    consumer.get.receive("result", new XdmAtomicValue(hlines), new XProcMetadata(MediaType.TEXT))
  }
}
