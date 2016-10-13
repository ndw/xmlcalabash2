package com.xmlcalabash.drivers

import java.io.FileWriter
import javax.xml.transform.sax.SAXSource

import com.xmlcalabash.core.XProcEngine
import com.jafpl.graph.{Graph, Runtime}
import com.xmlcalabash.items.StringItem
import com.xmlcalabash.model.xml.Parser
import com.xmlcalabash.xpath.{CR_xpath_31_20151217, XPathParser}
import net.sf.saxon.s9api.{Processor, QName}
import org.slf4j.LoggerFactory
import org.xml.sax.InputSource

object Main extends App {
  val logger = LoggerFactory.getLogger(this.getClass)
  val processor = new Processor(false)

  val href = "pipe.xpl"
  val builder = processor.newDocumentBuilder()
  val node = builder.build(new SAXSource(new InputSource(href)))
  val engine = new XProcEngine(processor)
  val parser = new Parser(engine)

  val model = parser.parse(node)
  val mdump = parser.dump(model)
  val pxw = new FileWriter("px.xml")
  pxw.write(mdump.toString)
  pxw.close()

  var graph: Graph = _
  graph = makeGraph
  run()

  private def makeGraph: Graph = {
    val graph = new Graph()
    model.buildGraph(graph, engine)

    if (graph.valid()) {
      println(graph.valid())
    }

    val pgw = new FileWriter("pg.xml")
    val gdump = graph.dump()
    pgw.write(gdump)
    pgw.close()

    graph
  }

  private def run(): Unit = {
    if (graph.valid()) {
      logger.info("Start your engines!")
      val graphRuntime = new Runtime(graph)
      graphRuntime.start()

      graphRuntime.write("source", new StringItem("Hello world"))
      graphRuntime.close("source")

      while (graphRuntime.running) {
        Thread.sleep(100)
      }

      var item = graphRuntime.read("result")
      while (item.isDefined) {
        println("OUTPUT:" + item.get)
        item = graphRuntime.read("result")
      }
    } else {
      println("Invalid graph")
    }
  }
}
