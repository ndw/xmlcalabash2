package com.xmlcalabash.drivers

import java.io.FileWriter
import java.net.URI
import javax.xml.transform.sax.SAXSource

import com.xmlcalabash.core.XProcEngine
import com.xmlcalabash.graph.Graph
import com.xmlcalabash.items.StringItem
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
  val graph = new Graph(engine)
  model.buildGraph(graph)

  val valid = graph.valid()
  println(valid)

  if (valid) {
    println(graph.dump())

    val w = new FileWriter("pg.xml")
    w.write(graph.dump().toString)
    w.close()

    /*
    graph.makeActors()
    for (node <- graph.inputs()) {
      node.write(new StringItem("Hello world"))
      node.close()
    }
    var running = true
    while (running) {
      Thread.sleep(100)
      running = !graph.finished
    }
    for (node <- graph.outputs()) {
      var item = node.read()
      while (item.isDefined) {
        println("OUTPUT:" + item.get)
        item = node.read()
      }
    }
    */
  }
}
