package com.xmlcalabash.drivers

import java.io.{File, PrintWriter}
import javax.xml.transform.sax.SAXSource

import com.jafpl.graph.Graph
import com.jafpl.messages.{ItemMessage, Metadata}
import com.jafpl.runtime.GraphRuntime
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ModelException, ParseException}
import com.xmlcalabash.model.xml.Parser
import com.xmlcalabash.runtime.{BufferingConsumer, PrintingConsumer, XProcMetadata}
import org.xml.sax.InputSource

object XmlTesting extends App {

  runOne()

  def runTwo(): Unit = {
    val data = "Hello, world."
    val xmlCalabash = XMLCalabash.newInstance()
    val builder = xmlCalabash.processor.newDocumentBuilder()
    val fn = "src/test/resources/simple.xpl"
    val source = new SAXSource(new InputSource(fn))

    builder.setDTDValidation(false)
    builder.setLineNumbering(true)

    val node = builder.build(source)

    val parserConfig = XMLCalabash.newInstance()

    val parser = new Parser(parserConfig)
    val pipeline = parser.parsePipeline(node)
    val graph = pipeline.pipelineGraph()

    graph.close()
    //dumpRaw(graph)
    dumpGraph(graph)

    val runtime = new GraphRuntime(graph, xmlCalabash)

    for (port <- pipeline.inputPorts) {
      runtime.inputs(port).send(new ItemMessage(data, new XProcMetadata("text/plain")))
    }

    val bc = new BufferingConsumer()
    for (port <- pipeline.outputPorts) {
      runtime.outputs(port).setConsumer(bc)
    }

    runtime.run()

    println(bc.items.size)
    for (item <- bc.items) {
      println("ITEM: " + item)
    }
  }

  def runOne(): Unit = {
    val xmlCalabash = XMLCalabash.newInstance()
    val builder = xmlCalabash.processor.newDocumentBuilder()
    val fn = "pipe.xpl"
    val source = new SAXSource(new InputSource(fn))

    builder.setDTDValidation(false)
    builder.setLineNumbering(true)

    val node = builder.build(source)

    var errored = false
    try {
      val parserConfig = XMLCalabash.newInstance()
      val parser = new Parser(parserConfig)
      val pipeline = parser.parsePipeline(node)
      //println(pipeline.asXML)
      val graph = pipeline.pipelineGraph()

      graph.close()
      //dumpRaw(graph)
      dumpGraph(graph)

      //System.exit(0)

      val runtime = new GraphRuntime(graph, xmlCalabash)

      for (port <- pipeline.inputPorts) {
        xmlCalabash.trace(s"Binding input port $port to 'Hello, world.'", "ExternalBindings")
        runtime.inputs(port).send(new ItemMessage("Hello, world.", Metadata.STRING))
      }

      for (port <- pipeline.outputPorts) {
        xmlCalabash.trace(s"Binding output port stdout", "ExternalBindings")
        val pc = new PrintingConsumer()
        runtime.outputs(port).setConsumer(pc)
      }

      for (bind <- pipeline.bindings) {
        xmlCalabash.trace(s"Binding option {$bind} to 'pipe'", "ExternalBindings")
        runtime.bindings(bind.getClarkName).set("pipe")
      }

      runtime.run()
    } catch {
      case t: Throwable =>
        println(s"caught error:$t")
        t match {
          case model: ModelException => Unit
          case parse: ParseException => Unit
          case _ => throw t
        }
        errored = true
    }

    if (errored) {
      System.exit(1)
    }
  }

  private def dumpGraph(graph: Graph): Unit = {
    dumpGraph(graph, None)
  }

  private def dumpGraph(graph: Graph, fn: String): Unit = {
    dumpGraph(graph, Some(fn))
  }

  private def dumpGraph(graph: Graph, fn: Option[String]): Unit = {
    val pw = new PrintWriter(new File(fn.getOrElse("/projects/github/xproc/meerschaum/pg.xml")))
    pw.write(graph.asXML.toString)
    pw.close()
  }

  private def dumpRaw(graph: Graph): Unit = {
    graph.dump()
  }

}
