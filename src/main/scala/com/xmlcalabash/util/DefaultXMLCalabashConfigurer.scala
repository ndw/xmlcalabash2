package com.xmlcalabash.util

import com.jafpl.util.DefaultTraceEventManager
import com.xmlcalabash.config.{Signatures, XMLCalabash, XMLCalabashConfigurer}
import com.xmlcalabash.parsers.StepConfigBuilder
import net.sf.saxon.s9api.Processor

class DefaultXMLCalabashConfigurer extends XMLCalabashConfigurer {
  val config = new XMLCalabashConfiguration()
  config.load()

  override def configure(configuration: XMLCalabash): Unit = {
    configuration.processor = new Processor(false)
    configuration.errorListener = new DefaultErrorListener()

    val signatures = new Signatures()
    val builder = new StepConfigBuilder()
    val sigs = builder.parse(getClass.getResourceAsStream("/xproc-steps.txt"))
    for (name <- sigs.stepTypes) {
      signatures.addStep(sigs.step(name))
    }
    configuration.signatures = signatures

    configuration.traceEventManager = new DefaultTraceEventManager()

    val resolver = new XProcURIResolver(configuration)
    configuration.uriResolver = resolver
    configuration.entityResolver = resolver
    configuration.unparsedTextURIResolver = resolver
    configuration.moduleURIResolver = resolver

    configuration.deliveryAgent = new DefaultDeliveryAgent(configuration)
    configuration.documentManager = new DefaultDocumentManager(configuration)
  }
}
