package com.xmlcalabash.testers

import com.jafpl.runtime.GraphRuntime
import com.xmlcalabash.exceptions.{ModelException, TestException}
import com.xmlcalabash.model.util.ParserConfiguration
import com.xmlcalabash.model.xml.Parser
import com.xmlcalabash.runtime.{BufferingConsumer, DevNullConsumer, SaxonRuntimeConfiguration, XmlMetadata}
import com.xmlcalabash.util.Schematron
import net.sf.saxon.s9api.{QName, XdmItem, XdmNode}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Tester(parserConfig: ParserConfiguration, runtimeConfig: SaxonRuntimeConfiguration) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var _pipeline = Option.empty[XdmNode]
  private var _schematron = Option.empty[XdmNode]
  private var _inputs   = mutable.HashMap.empty[String, ListBuffer[XdmNode]]
  private var _bindings = mutable.HashMap.empty[String, XdmItem]
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

  def addBinding(optname: QName, item: XdmItem): Unit = {
    _bindings.put(optname.getClarkName, item)
  }

  def run(): Option[String] = {
    if (_pipeline.isEmpty) {
      throw new TestException("No pipeline specified")
    }

    val processor = runtimeConfig.processor
    val builder = processor.newDocumentBuilder()

    try {
      val parser = new Parser(parserConfig)
      val pipeline = parser.parsePipeline(_pipeline.get)
      val graph = pipeline.pipelineGraph()
      graph.close()

      val runtime = new GraphRuntime(graph, runtimeConfig)

      for (port <- pipeline.inputPorts) {
        if (_inputs.contains(port)) {
          for (item <- _inputs(port)) {
            runtime.inputs(port).receive("source", item, new XmlMetadata())
          }
        } else {
          logger.warn(s"No inputs specified for $port")
        }
      }

      for (bind <- pipeline.bindings) {
        if (_bindings.contains(bind.getClarkName)) {
          runtime.bindings(bind.getClarkName).set(_bindings(bind.getClarkName))
        } else {
          logger.warn(s"No binding specified for $bind")
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

      runtime.run()

      val resultDoc = result.get.items.head.asInstanceOf[XdmNode]

      if (_schematron.isDefined) {
        val schematest = new Schematron(runtimeConfig)
        val results = schematest.test(resultDoc, schematron.get)
        for (result <- results) {
          val xpath = result.getAttributeValue(_test)
          val text = result.getStringValue
          logger.info(s"FAIL: ${_pipeline.get.getBaseURI}: $xpath: $text")
        }
        if (results.isEmpty) {
          logger.info(s"PASS: ${_pipeline.get.getBaseURI}")
          None
        } else {
          Some("SCHEMATRON")
        }
      } else {
        logger.info(s"NONE: ${_pipeline.get.getBaseURI}")
        None
      }
    } catch {
      case model: ModelException =>
        Some(model.code)
      case t: Throwable => throw t
    }
  }
}