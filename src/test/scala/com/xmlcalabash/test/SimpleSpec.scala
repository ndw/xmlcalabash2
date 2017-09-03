package com.xmlcalabash.test

import javax.xml.transform.sax.SAXSource

import com.jafpl.messages.Metadata
import com.jafpl.runtime.GraphRuntime
import com.xmlcalabash.model.util.DefaultParserConfiguration
import com.xmlcalabash.model.xml.Parser
import com.xmlcalabash.runtime.{BufferingConsumer, SaxonRuntimeConfiguration, XmlMetadata}
import net.sf.saxon.s9api.Processor
import org.scalatest.FlatSpec
import org.xml.sax.InputSource

class SimpleSpec extends FlatSpec {

  "A simple, identity pipeline " should " run " in {
    val data = "Hello, world."
    val processor = new Processor(false)
    val builder = processor.newDocumentBuilder()
    val fn = "src/test/resources/simple.xpl"
    val source = new SAXSource(new InputSource(fn))

    builder.setDTDValidation(false)
    builder.setLineNumbering(true)

    val node = builder.build(source)

    val parserConfig = new DefaultParserConfiguration()

    val parser = new Parser(parserConfig)
    val pipeline = parser.parsePipeline(node)
    val graph = pipeline.pipelineGraph()

    val runtimeConfig = new SaxonRuntimeConfiguration(processor)
    val runtime = new GraphRuntime(graph, runtimeConfig)

    for (port <- pipeline.inputPorts) {
      runtime.inputs(port).send(data, new XmlMetadata("text/plain"))
    }

    val bc = new BufferingConsumer()
    for (port <- pipeline.outputPorts) {
      runtime.outputs(port).setConsumer(bc)
    }

    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == data)

  }

}
