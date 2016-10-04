package com.xmlcalabash.drivers

import java.net.URI
import javax.xml.transform.sax.SAXSource

import com.xmlcalabash.core.XProcEngine
import com.xmlcalabash.model.xml.XProc11Parser
import net.sf.saxon.s9api.Processor
import org.xml.sax.InputSource

object Main extends App {
  val processor = new Processor(false)

  val href = "pipe.xpl"
  val builder = processor.newDocumentBuilder()
  val node = builder.build(new SAXSource(new InputSource(href)))
  val engine = new XProcEngine(processor)
  val parser = new XProc11Parser(engine)

  val decl = parser.parse(node)

  if (decl.isDefined) {
    val dump = decl.get.dump(engine)
    println(dump)
  }

  println("Yeah, what of it?")
}
