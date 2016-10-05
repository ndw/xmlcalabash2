package com.xmlcalabash.drivers

import java.net.URI
import javax.xml.transform.sax.SAXSource

import com.xmlcalabash.core.XProcEngine
import com.xmlcalabash.model.xml.Parser
import net.sf.saxon.s9api.Processor
import org.xml.sax.InputSource

object Main extends App {
  val processor = new Processor(false)

  val href = "pipe.xpl"
  val builder = processor.newDocumentBuilder()
  val node = builder.build(new SAXSource(new InputSource(href)))
  val engine = new XProcEngine(processor)
  val parser = new Parser(engine)

  val model = parser.parse(node)

}
