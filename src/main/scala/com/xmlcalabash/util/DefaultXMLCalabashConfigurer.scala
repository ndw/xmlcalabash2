package com.xmlcalabash.util

import java.util.Properties

import com.jafpl.util.DefaultTraceEventManager
import com.xmlcalabash.config.{XMLCalabashConfig, XMLCalabashConfigurer}
import net.sf.saxon.s9api.{Processor, QName}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class DefaultXMLCalabashConfigurer extends XMLCalabashConfigurer {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val config = new XMLCalabashConfiguration()
  config.load()

  override def configure(configuration: XMLCalabashConfig): Unit = {
    configuration.processor = new Processor(false)
    configuration.errorListener = new DefaultErrorListener()

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

    configuration.documentManager = new DefaultDocumentManager(configuration)

    loadProperties(configuration)
  }

  private def loadProperties(configuration: XMLCalabashConfig): Unit = {
    val uriEnum = this.getClass.getClassLoader.getResources("com.xmlcalabash.properties")
    while (uriEnum.hasMoreElements) {
      val url = uriEnum.nextElement()
      logger.debug(s"Loading properties: $url")

      val conn = url.openConnection()
      val stream = conn.getInputStream
      val props = new Properties()
      props.load(stream)

      val nsmap = mutable.HashMap.empty[String,String]
      val NSPattern = "namespace\\s+(.+)$".r
      val FPattern = "function\\s+(.+):(.+)$".r
      val SPattern = "step\\s+(.+):(.+)$".r

      // Properties are unordered so find the namespace bindings
      var propIter = props.stringPropertyNames().iterator()
      while (propIter.hasNext) {
        val name = propIter.next()
        val value = props.get(name).asInstanceOf[String]
        value match {
          case NSPattern(uri) =>
            if (nsmap.contains(name)) {
              throw new RuntimeException("Cannot redefine namespace bindings in property file")
            }
            nsmap.put(name, uri)
          case _ => Unit
        }
      }

      // Now parse the step and function declarations
      propIter = props.stringPropertyNames().iterator()
      while (propIter.hasNext) {
        val name = propIter.next()
        val value = props.get(name).asInstanceOf[String]
        value match {
          case NSPattern(uri) => Unit
          case FPattern(pfx,local) =>
            if (nsmap.contains(pfx)) {
              val qname = new QName(pfx, nsmap(pfx), local)
              configuration.implementFunction(qname, name)
            } else {
              logger.debug(s"No namespace binding for $pfx, ignoring: $name=$value")
            }
          case SPattern(pfx,local) =>
            if (nsmap.contains(pfx)) {
              val qname = new QName(pfx, nsmap(pfx), local)
              configuration.implementAtomicStep(qname, name)
            } else {
              logger.debug(s"No namespace binding for $pfx, ignoring: $name=$value")
            }
          case _ =>
            logger.debug(s"Unparseable property, ignoring: $name=$value")
        }
      }
    }
  }
}
