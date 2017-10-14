package com.xmlcalabash.drivers

import java.io.{File, PrintWriter}
import javax.xml.transform.sax.SAXSource

import com.jafpl.exceptions.GraphException
import com.jafpl.graph.Graph
import com.jafpl.messages.Message
import com.jafpl.runtime.GraphRuntime
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ModelException, ParseException, StepException, XProcException}
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.xml.{DeclareStep, Parser}
import com.xmlcalabash.runtime.{ExpressionContext, PrintingConsumer, XProcMetadata, XProcXPathExpression}
import com.xmlcalabash.util.{ArgBundle, URIUtils, XProcVarValue}
import net.sf.saxon.s9api.QName
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

    for (injectable <- options.injectables) {
      val doc = builder.build(new SAXSource(new InputSource(injectable)))
      parser.parseInjectables(doc)
    }

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
      if (options.inputs.contains(port)) {
        for (fn <- options.inputs(port)) {
          val node = xmlCalabash.parse(fn, URIUtils.cwdAsURI)
          runtime.inputs(port).send(new XPathItemMessage(node, XProcMetadata.XML, ExpressionContext.NONE))
        }
      } else {
        throw XProcException.xiNoBindingForPort(port)
      }
    }

    for (port <- pipeline.outputPorts) {
      xmlCalabash.trace(s"Binding output port stdout", "ExternalBindings")
      val outputs = options.outputs.get(port)
      val serOpt = pipeline.output(port).get.serialization
      val pc = if (outputs.isDefined) {
        new PrintingConsumer(xmlCalabash, serOpt, outputs.get)
      } else {
        new PrintingConsumer(xmlCalabash, serOpt)
      }
      runtime.outputs(port).setConsumer(pc)
    }

    processOptionBindings(runtime, pipeline)

    runtime.run()
  } catch {
    case t: Throwable =>
      if (options.debug) {
        t.printStackTrace()
      }

      t match {
        case model: ModelException =>
          println(model)
        case parse: ParseException =>
          println(parse)
        case graph: GraphException =>
          println(graph)
        case xproc: XProcException =>
          val code = xproc.code
          val message = if (xproc.message.isDefined) {
            xproc.message.get
          } else {
            code match {
              case qname: QName =>
                xmlCalabash.errorExplanation.message(qname, xproc.details)
              case _ =>
                s"Configuration error: code ($code) is not a QName"
            }
          }
          if (xproc.location.isDefined) {
            println(s"ERROR ${xproc.location.get} $code $message")
          } else {
            println(s"ERROR $code $message")
          }

          if (options.verbose && code.isInstanceOf[QName]) {
            val explanation = xmlCalabash.errorExplanation.explanation(code.asInstanceOf[QName])
            if (explanation != "") {
              println(explanation)
            }
          }
        case step: StepException =>
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

  private def processOptionBindings(runtime: GraphRuntime, pipeline: DeclareStep): Unit = {
    val bindingsMap = mutable.HashMap.empty[String, Message]
    for (bind <- pipeline.bindings) {
      val jcbind = bind.getClarkName

      if (options.params.contains(bind)) {
        xmlCalabash.trace(s"Binding option $bind to '${options.params(bind)}'", "ExternalBindings")
        val value = options.params(bind)
        val msg = new XPathItemMessage(value.value, XProcMetadata.XML, value.context)
        runtime.bindings(jcbind).set(value)
        bindingsMap.put(jcbind, msg)
      } else {
        xmlCalabash.trace(s"No binding provided for option $bind; using default", "ExternalBindings")
        val decl = pipeline.bindingDeclaration(bind)
        if (decl.isDefined) {
          if (decl.get.select.isDefined) {
            val context = new ExpressionContext(None, options.inScopeNamespaces, None)
            val expr = new XProcXPathExpression(context, decl.get.select.get)
            val msg = xmlCalabash.expressionEvaluator.singletonValue(expr, List(), bindingsMap.toMap, None)
            val eval = msg.asInstanceOf[XPathItemMessage].item
            runtime.bindings(jcbind).set(new XProcVarValue(eval, context))
            bindingsMap.put(jcbind, msg)
          } else {
            if (decl.get.required) {
              throw XProcException.staticError(18, bind.toString, pipeline.location)
            } else {
              xmlCalabash.trace(s"No default for option $bind", "ExternalBindings")
            }
          }
        } else {
          println("No decl for " + bind + " ???")
        }
      }
    }

    for (bind <- options.params.keySet) {
      val jcbind = bind.getClarkName
      if (!bindingsMap.contains(jcbind)) {
        println(s"Ignoring unused binding for $bind")
      }
    }
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
