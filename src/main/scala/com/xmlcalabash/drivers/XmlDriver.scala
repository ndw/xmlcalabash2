package com.xmlcalabash.drivers

import javax.xml.transform.sax.SAXSource

import com.xmlcalabash.model.util.DefaultErrorListener
import com.xmlcalabash.model.xml.Parser
import net.sf.saxon.s9api.Processor
import org.xml.sax.InputSource

object XmlDriver extends App {
  private val processor = new Processor(false)
  private val builder = processor.newDocumentBuilder()
  private val fn = "pipe.xpl"
  private val source = new SAXSource(new InputSource(fn))

  builder.setDTDValidation(false)
  builder.setLineNumbering(true)

  private val node = builder.build(source)

  val listener = new DefaultErrorListener()

  val parser = new Parser(listener)
  val pipeline = parser.parsePipeline(node)
  println(pipeline)
}
