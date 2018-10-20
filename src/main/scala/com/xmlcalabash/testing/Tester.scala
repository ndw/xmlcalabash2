package com.xmlcalabash.testing

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.{TestException, XProcException}
import com.xmlcalabash.messages.XdmNodeItemMessage
import com.xmlcalabash.runtime.{BufferingConsumer, ExpressionContext, XMLCalabashRuntime, XProcMetadata}
import com.xmlcalabash.util.XProcVarValue
import net.sf.saxon.s9api.{QName, XdmNode, XdmValue}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Tester(runtimeConfig: XMLCalabashConfig) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var _pipeline = Option.empty[XdmNode]
  private var _schematron = Option.empty[XdmNode]
  private var _inputs   = mutable.HashMap.empty[String, ListBuffer[XdmNode]]
  private var _bindings = mutable.HashMap.empty[QName, XdmValue]
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
    _bindings.put(optname, item)
  }

  def run(): TestResult = {
    if (_pipeline.isEmpty) {
      throw new TestException("No pipeline specified")
    }

    var runtime: XMLCalabashRuntime = null
    try {
      runtime = runtimeConfig.runtime(_pipeline.get)
      val result = new BufferingConsumer()

      for (port <- _inputs.keySet) {
        for (item <- _inputs(port)) {
          runtime.input(port, new XdmNodeItemMessage(item, new XProcMetadata()))
        }
      }

      runtime.output("result", result)

      for (bind <- _bindings.keySet) {
        runtime.option(bind, new XProcVarValue(_bindings(bind), ExpressionContext.NONE))
      }

      runtime.run()

      val resultDoc = result.messages.head.item.asInstanceOf[XdmNode]

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
          runtime.stop()
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
          runtime.stop()
        }
        new TestResult(ex)
    }
  }
}