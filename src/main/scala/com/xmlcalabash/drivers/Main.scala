package com.xmlcalabash.drivers

import java.io.FileWriter
import javax.xml.transform.sax.SAXSource

import com.jafpl.graph.{Graph, Runtime}
import com.xmlcalabash.core.XProcEngine
import com.xmlcalabash.items.{StringItem, XPathDataModelItem}
import com.xmlcalabash.model.xml.{Artifact, Parser, XMLErrorListener}
import com.xmlcalabash.model.xml.decl.XProc11Steps
import net.sf.saxon.s9api.Processor
import org.slf4j.LoggerFactory
import org.xml.sax.InputSource

object Main extends App {
  val logger = LoggerFactory.getLogger(this.getClass)
  val processor = new Processor(false)

  val href = "pipe.xpl"
  val builder = processor.newDocumentBuilder()
  builder.setLineNumbering(true)
  builder.setDTDValidation(false)
  val node = builder.build(new SAXSource(new InputSource(href)))
  val engine = new XProcEngine(processor)
  val parseErrorListener = new XMLErrorListener()
  val parser = new Parser(engine, parseErrorListener)

  val model = parser.parse(node, List(new XProc11Steps()))

  if (model.isDefined) {
    val mdump = parser.dump(model.get)
    val pxw = new FileWriter("px.xml")
    pxw.write(mdump.toString)
    pxw.close()

    val graph = makeGraph(model.get)

    if (graph.isDefined) {
      run(graph.get)
    }
  }

  private def makeGraph(model: Artifact): Option[Graph] = {
    val graph = new Graph()
    model.buildGraph(graph, engine)

    val valid = graph.valid()

    val pgw = new FileWriter("pg.xml")
    val gdump = graph.dump()
    pgw.write(gdump)
    pgw.close()

    if (valid) {
      Some(graph)
    } else {
      None
    }
  }

  private def run(graph: Graph): Unit = {
    logger.info("Start your engines!")
    val graphRuntime = new Runtime(graph)
    graphRuntime.start()

    for (input <- graphRuntime.inputs()) {
      //println("==input=> " + input.port)
      if (input.port == "source") {
        graphRuntime.write(input.port, new StringItem("Hello world"))
      }
      if (input.port == "{}fred") {
        graphRuntime.write(input.port, new XPathDataModelItem(engine.getUntypedAtomic("-1")))
      }
      graphRuntime.close(input.port)
    }

    while (graphRuntime.running) {
      Thread.sleep(100)
    }

    var item = graphRuntime.read("result")
    while (item.isDefined) {
      println("OUTPUT:" + item.get)
      item = graphRuntime.read("result")
    }
  }
}
