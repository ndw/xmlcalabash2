package com.xmlcalabash.drivers

import java.io.{File, PrintWriter}
import javax.xml.transform.sax.SAXSource

import com.jafpl.exceptions.GraphException
import com.jafpl.graph.Graph
import com.jafpl.messages.{ItemMessage, Message, Metadata}
import com.jafpl.runtime.GraphRuntime
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ModelException, ParseException, StepException}
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.xml.{DeclareStep, Parser}
import com.xmlcalabash.runtime.{ExpressionContext, PrintingConsumer, XProcXPathExpression}
import com.xmlcalabash.util.ArgBundle
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem}
import org.xml.sax.InputSource

import scala.collection.mutable

object XmlDriver extends App {
  type OptionMap = Map[Symbol, Any]

  private val xmlCalabash = XMLCalabash.newInstance()

  val options = new ArgBundle(xmlCalabash, args.toList)

  val builder = xmlCalabash.processor.newDocumentBuilder()
  val source = new SAXSource(new InputSource(options.pipeline))
  builder.setDTDValidation(false)
  builder.setLineNumbering(true)

  val node = builder.build(source)

  var errored = false
  try {
    val parser = new Parser(xmlCalabash)
    val pipeline = parser.parsePipeline(node)

    val graph = pipeline.pipelineGraph()

    if (options.dumpXML.isDefined) {
      dumpXML(pipeline, options.dumpXML.get)
    }

    if (options.graphBefore.isDefined) {
      dumpGraph(graph, options.graphBefore.get)
    }

    graph.close()

    if (options.raw) {
      dumpRaw(graph)
    }

    if (options.graph.isDefined) {
      dumpGraph(graph, options.graph.get)
    }

    if (options.norun) {
      System.exit(0)
    }

    val runtime = new GraphRuntime(graph, xmlCalabash)
    runtime.traceEventManager = xmlCalabash.traceEventManager

    for (port <- pipeline.inputPorts) {
      xmlCalabash.trace(s"Binding input port $port to 'Hello, world.'", "ExternalBindings")
      runtime.inputs(port).send(new ItemMessage("Hello, world.", Metadata.STRING))
    }

    for (port <- pipeline.outputPorts) {
      xmlCalabash.trace(s"Binding output port stdout", "ExternalBindings")
      val pc = new PrintingConsumer()
      runtime.outputs(port).setConsumer(pc)
    }

    val usedBindings = mutable.HashSet.empty[QName]
    for (bind <- pipeline.bindings) {
      if (options.params.contains(bind)) {
        xmlCalabash.trace(s"Binding option $bind to '${options.params(bind)}'", "ExternalBindings")
        runtime.bindings(bind.getClarkName).set(options.params(bind))
        usedBindings += bind
      } else {
        println(s"Missing binding for $bind, supplied nothing")
      }
    }
    for (bind <- options.params.keySet) {
      if (!usedBindings.contains(bind)) {
        println(s"Ignoring unused binding for $bind")
      }
    }

    runtime.run()
  } catch {
    case t: Throwable =>
      t match {
        case model: ModelException => Unit
          println(model)
        case parse: ParseException => Unit
          println(parse)
        case graph: GraphException => Unit
          println(graph)
        case step: StepException => Unit
          val code = step.code
          val message = if (step.message.isDefined) {
            step.message.get
          } else {
            xmlCalabash.errorExplanation.message(code)
          }
          println(s"ERROR $code $message")

          if (options.verbose) {
            val explanation = xmlCalabash.errorExplanation.explanation(code)
            if (explanation != "") {
              println(explanation)
            }
          }

        case _ =>
          println("Caught unexpected error: " + t)
          t.printStackTrace()
          throw t
      }
      errored = true
  }

  if (errored) {
    System.exit(1)
  }

  // ===========================================================================================

  private def dumpGraph(graph: Graph, fn: String): Unit = {
    val pw = new PrintWriter(new File(fn))
    pw.write(graph.asXML.toString)
    pw.close()
  }

  private def dumpXML(pipeline: DeclareStep, fn: String): Unit = {
    val pw = new PrintWriter(new File(fn))
    pw.write(pipeline.asXML.toString)
    pw.close()
  }

  private def dumpRaw(graph: Graph): Unit = {
    graph.dump()
  }
}
