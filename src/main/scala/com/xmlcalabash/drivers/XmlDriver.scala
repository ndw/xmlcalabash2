package com.xmlcalabash.drivers

import javax.xml.transform.sax.SAXSource

import com.jafpl.messages.Metadata
import com.jafpl.runtime.GraphRuntime
import com.xmlcalabash.model.util.DefaultParserConfiguration
import com.xmlcalabash.model.xml.Parser
import com.xmlcalabash.runtime.{PrintingConsumer, SaxonRuntimeConfiguration}
import net.sf.saxon.s9api.Processor
import org.xml.sax.InputSource

object XmlDriver extends App {
  private val processor = new Processor(false)
  private val builder = processor.newDocumentBuilder()
  private val fn = "pipe.xpl"
  private val source = new SAXSource(new InputSource(fn))

  builder.setDTDValidation(false)
  builder.setLineNumbering(true)

  private val node = builder.build(source)

  val parserConfig = new DefaultParserConfiguration()

  val parser = new Parser(parserConfig)
  val pipeline = parser.parsePipeline(node)
  //println(pipeline.asXML)
  val graph = pipeline.pipelineGraph()
  //println(graph.asXML)

  val runtimeConfig = new SaxonRuntimeConfiguration(processor)
  val runtime = new GraphRuntime(graph, runtimeConfig)

  for (port <- pipeline.inputPorts) {
    println(s"Binding input port $port to 'Hello, world.'")
    runtime.inputs(port).receive("source", "Hello, world.", Metadata.STRING)
  }

  for (port <- pipeline.outputPorts) {
    println(s"Binding output port $port to stdout")
    val pc = new PrintingConsumer()
    runtime.outputs(port).setConsumer(pc)
  }

  runtime.run()
}
