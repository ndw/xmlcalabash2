package com.xmlcalabash.testers

import com.jafpl.exceptions.JafplException
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.GraphRuntime
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.drivers.XmlDriver.xmlCalabash
import com.xmlcalabash.exceptions.{ModelException, StepException, TestException, XProcException}
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.xml.{DeclareStep, Parser}
import com.xmlcalabash.runtime.{BufferingConsumer, DevNullConsumer, ExpressionContext, XProcMetadata, XProcXPathExpression}
import com.xmlcalabash.util.{Schematron, XProcVarValue}
import net.sf.saxon.s9api.{QName, XdmItem, XdmNode, XdmValue}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Tester(runtimeConfig: XMLCalabashConfig) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var _pipeline = Option.empty[XdmNode]
  private var _schematron = Option.empty[XdmNode]
  private var _inputs   = mutable.HashMap.empty[String, ListBuffer[XdmNode]]
  private var _bindings = mutable.HashMap.empty[String, XdmValue]
  private var _tests    = Option.empty[String]
  private val _test     = new QName("", "test")

  def pipeline: Option[XdmNode] = _pipeline
  def pipeline_=(pipe: XdmNode): Unit = {
    if (_pipeline.isEmpty) {
      _pipeline = Some(pipe)
    } else {
      throw new TestException("Cannot reset pipeline in test")
    }
  }

  def schematron: Option[XdmNode] = _schematron
  def schematron_=(schema: XdmNode): Unit = {
    if (_schematron.isEmpty) {
      _schematron = Some(schema)
    } else {
      throw new TestException("Cannot reset schematron in test")
    }
  }

  def addInput(port: String, item: XdmNode): Unit = {
    if (_inputs.contains(port)) {
      _inputs(port) += item
    } else {
      val list = ListBuffer.empty[XdmNode]
      list += item
      _inputs.put(port, list)
    }
  }

  def addBinding(optname: QName, item: XdmValue): Unit = {
    _bindings.put(optname.getClarkName, item)
  }

  // Return None if the test passed, otherwise return the failure error code
  def run(): TestResult = {
    if (_pipeline.isEmpty) {
      throw new TestException("No pipeline specified")
    }

    val processor = runtimeConfig.processor
    val builder = processor.newDocumentBuilder()
    var runtime: GraphRuntime = null

    try {
      val parser = new Parser(runtimeConfig)
      val pipeline = parser.parsePipeline(_pipeline.get)
      val graph = pipeline.pipelineGraph()
      graph.close()

      runtime = new GraphRuntime(graph, runtimeConfig)

      for (port <- pipeline.inputPorts) {
        if (_inputs.contains(port)) {
          for (item <- _inputs(port)) {
            runtime.inputs(port).send(new ItemMessage(item, new XProcMetadata()))
          }
        } else {
          //logger.warn(s"No inputs specified for $port")
        }
      }

      var result = Option.empty[BufferingConsumer]
      for (port <- pipeline.outputPorts) {
        if (port == "result") {
          result = Some(new BufferingConsumer())
          runtime.outputs(port).setConsumer(result.get)
        } else {
          logger.warn(s"Unexpected output ignored: $port")
          runtime.outputs(port).setConsumer(new DevNullConsumer())
        }
      }

      processOptionBindings(runtime, pipeline)

      runtime.run()

      val resultDoc = result.get.items.head.asInstanceOf[XdmNode]

      //println(resultDoc)

      if (_schematron.isDefined) {
        var fail = ""
        val schematest = new Schematron(runtimeConfig)
        val results = schematest.test(resultDoc, schematron.get)
        for (result <- results) {
          val xpath = result.getAttributeValue(_test)
          val text = result.getStringValue
          if (fail == "") {
            fail = s"$xpath: $text"
          }
        }
        if (results.isEmpty) {
          new TestResult(true)
        } else {
          if (fail == "") {
            new TestResult(false, "SCHEMATRON")
          } else {
            new TestResult(false, fail)
          }
        }
      } else {
        logger.info(s"NONE: ${_pipeline.get.getBaseURI}")
        new TestResult(true)
      }
    } catch {
      case xproc: XProcException =>
        if (runtime != null) {
          runtime.stop
        }

        val code = xproc.code
        val message = if (xproc.message.isDefined) {
          xproc.message.get
        } else {
          code match {
            case qname: QName =>
              runtimeConfig.errorExplanation.message(qname, xproc.details)
            case _ =>
              s"Configuration error: code ($code) is not a QName"
          }
        }

        if (xproc.location.isDefined) {
          println(s"ERROR ${xproc.location.get} $code $message")
        } else {
          println(s"ERROR $code $message")
        }

        new TestResult(xproc)
      case ex: Exception =>
        if (runtime != null) {
          runtime.stop
        }
        ex.printStackTrace(Console.err)
        new TestResult(ex)
    }
  }

  private def processOptionBindings(runtime: GraphRuntime, pipeline: DeclareStep): Unit = {
    val bindingsMap = mutable.HashMap.empty[String, Message]
    for (bind <- pipeline.bindings) {
      val jcbind = bind.getClarkName

      if (_bindings.contains(bind.getClarkName)) {
        val value = _bindings(jcbind)
        val msg = new XPathItemMessage(value, XProcMetadata.XML, ExpressionContext.NONE)
        runtime.setOption(jcbind, value)
        bindingsMap.put(jcbind, msg)
      } else {
        val decl = pipeline.bindingDeclaration(bind)
        if (decl.isDefined) {
          if (decl.get.select.isDefined) {
            val context = ExpressionContext.NONE // new ExpressionContext(None, options.inScopeNamespaces, None)
            val expr = new XProcXPathExpression(context, decl.get.select.get)
            val msg = runtimeConfig.expressionEvaluator.singletonValue(expr, List(), bindingsMap.toMap, None)
            val eval = msg.asInstanceOf[XPathItemMessage].item
            runtime.setOption(jcbind, new XProcVarValue(eval, context))
            bindingsMap.put(jcbind, msg)
          } else {
            if (decl.get.required) {
              throw XProcException.staticError(18, bind.toString, pipeline.location)
            } else {
              val context = ExpressionContext.NONE
              val expr = new XProcXPathExpression(context, "()")
              val msg = runtimeConfig.expressionEvaluator.value(expr, List(), bindingsMap.toMap, None)
              val eval = msg.asInstanceOf[XPathItemMessage].item
              runtime.setOption(jcbind, new XProcVarValue(eval, context))
              bindingsMap.put(jcbind, msg)
            }
          }
        } else {
          println("No decl for " + bind + " ???")
        }
      }
    }
  }
}
