package com.xmlcalabash.drivers

import java.io.{File, PrintWriter}
import javax.xml.transform.sax.SAXSource

import com.jafpl.graph.Graph
import com.jafpl.messages.{BindingMessage, ItemMessage, Message, Metadata}
import com.jafpl.runtime.GraphRuntime
import com.sun.javafx.css.CssError.StringParsingError
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ModelException, ParseException}
import com.xmlcalabash.model.util.StringParsers
import com.xmlcalabash.model.xml.Parser
import com.xmlcalabash.runtime.{PrintingConsumer, XProcXPathExpression}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem}
import org.xml.sax.InputSource

import scala.collection.mutable

object XmlDriver extends App {
  type OptionMap = Map[Symbol, Any]

  private val xmlCalabash = XMLCalabash.newInstance()

  val options = optionMap(args.toList)

  val builder = xmlCalabash.processor.newDocumentBuilder()
  val fn = "pipe.xpl"
  val source = new SAXSource(new InputSource(fn))

  builder.setDTDValidation(false)
  builder.setLineNumbering(true)

  val node = builder.build(source)

  var errored = false
  try {
    val parser = new Parser(xmlCalabash)
    val pipeline = parser.parsePipeline(node)
    //println(pipeline.asXML)
    val graph = pipeline.pipelineGraph()

    graph.close()
    //dumpRaw(graph)
    dumpGraph(graph)

    //System.exit(0)

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

    for (bind <- pipeline.bindings) {
      xmlCalabash.trace(s"Binding option $bind to 'pipe'", "ExternalBindings")
      runtime.bindings(bind.getClarkName).set(new XdmAtomicValue("pipe"))
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

  // ===========================================================================================
  def optionMap(args: List[String]): OptionMap = {
    // -iport=input | --input port=input
    // -d[content-type@]port=data | --data [content-type@]port=data
    // -oport=output | --output port=output
    // -bprefix=namespace
    // param=string value
    // +param=file value
    // ?param=xpath expression value

    val map = mutable.HashMap.empty[Symbol, Any]
    val longPortRegex   = "(--input)|(--output)|(--data)".r
    val shortPortRegex  = "-([iod])(\\S+)=(.*)".r
    val nsbindingRegex  = "-(b)(\\S+)=(.*)".r
    val paramRegex      = "([\\+\\?])?(\\S+)=(\\S+)".r
    val pipelineRegex   = "([^-]).*".r
    var pos = 0
    while (pos < args.length) {
      val opt = args(pos)
      opt match {
        case longPortRegex(kind) =>
          println(s"long $kind: ${args(pos+1)}")
          pos += 2
        case shortPortRegex(kind, port, value) =>
          println(s"short $kind $port=$value")
          pos += 1
        case nsbindingRegex(prefix, uri) =>
          println(s"ns $prefix=$uri")
          val curBindings = map.getOrElse('nsbindings, Map()).asInstanceOf[Map[String,String]]
          val binding = mutable.HashMap.empty[String,String]
          binding.put(prefix,uri)
          map.put('nsbindings, curBindings ++ binding)
          pos += 1
        case paramRegex(kind, name, value) =>
          kind match {
            case "+" =>
              println(s"$name=file:$value")
            case "?" =>
              val nsbindings = if (map.contains('nsbindings)) {
                map.get('nsbindings).asInstanceOf[Map[String,String]]
              } else {
                Map.empty[String,String]
              }

              val curParams = map.getOrElse('params, Map()).asInstanceOf[Map[QName,XdmItem]]
              val paramBind = mutable.HashMap.empty[String, Message]
              for ((qname, value) <- curParams) {
                val clark = qname.getClarkName
                val msg = new BindingMessage(clark, new ItemMessage(value, Metadata.ANY))
                paramBind.put(clark, msg)
              }

              val expr = new XProcXPathExpression(nsbindings, value)
              val eval = xmlCalabash.expressionEvaluator.value(expr, List(), paramBind.toMap).asInstanceOf[XdmItem]
              println(s"$name=xpath:$eval")

              val param = mutable.HashMap.empty[QName, XdmItem]
              param.put(new QName("", name), eval)
              map.put('params, curParams ++ param)
            case null =>
              println(s"$name=$value")
              val curParams = map.getOrElse('params, Map()).asInstanceOf[Map[QName,XdmItem]]
              val param = mutable.HashMap.empty[QName, XdmItem]
              param.put(new QName("", name), new XdmAtomicValue(value))
              map.put('params, curParams ++ param)
            case _ =>
              println(s"??? $kind $name=$value")
          }
          pos += 1
        case pipelineRegex(fn) =>
          map.put('pipeline, fn)
          pos += 1
        case _ =>
          println(s"Unexpected $opt")
          pos += 1
      }
    }
    map.toMap
  }

  // ===========================================================================================

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
