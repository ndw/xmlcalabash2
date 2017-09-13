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

    val builder = new StepConfigBuilder()
    configuration.signatures = builder.parse(getClass.getResourceAsStream("/xproc-steps.txt"))

    configuration.traceEventManager = new DefaultTraceEventManager()
    val traces = Option(System.getProperty("com.xmlcalabash.trace")).getOrElse("")
    for (trace <- traces.split("\\s*,\\s*")) {
      if (trace.startsWith("-")) {
        configuration.traceEventManager.disableTrace(trace.substring(1))
      } else {
        if (trace.startsWith("+")) {
          configuration.traceEventManager.enableTrace(trace.substring(1))
        } else {
          configuration.traceEventManager.enableTrace(trace)
        }
      }
    }

    val timeout = Option(System.getProperty("com.xmlcalabash.watchdogTimeout")).getOrElse("1000")
    configuration.watchdogTimeout = timeout.toLong

    val resolver = new XProcURIResolver(configuration)
    configuration.uriResolver = resolver
    configuration.entityResolver = resolver
    configuration.unparsedTextURIResolver = resolver
    configuration.moduleURIResolver = resolver

    configuration.errorExplanation = new DefaultErrorExplanation(configuration)

    configuration.deliveryAgent = new DefaultDeliveryAgent(configuration)
    configuration.documentManager = new DefaultDocumentManager(configuration)
  }
}
