package com.xmlcalabash.util

import java.io.{File, FileInputStream}
import java.util.Properties

import com.jafpl.util.DefaultTraceEventManager
import com.xmlcalabash.config.{XMLCalabash, XMLCalabashConfigurer}
import com.xmlcalabash.model.xml.Parser
import javax.xml.transform.sax.SAXSource
import net.sf.saxon.s9api.{Processor, QName}
import org.xml.sax.InputSource

import scala.collection.mutable

class DefaultXMLCalabashConfigurer extends XMLCalabashConfigurer {
  val config = new XMLCalabashConfiguration()
  config.load()

  override def configure(configuration: XMLCalabash): Unit = {
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

    val xmlbuilder = configuration.processor.newDocumentBuilder()
    val stream = getClass.getResourceAsStream("/standard-steps.xpl")
    val source = new SAXSource(new InputSource(stream))
    xmlbuilder.setDTDValidation(false)
    xmlbuilder.setLineNumbering(true)
    val node = xmlbuilder.build(source)

    val parser = new Parser(configuration)
    configuration.signatures = parser.signatures(node)
  }

  private def loadProperties(configuration: XMLCalabash): Unit = {
    val uriEnum = this.getClass.getClassLoader.getResources("com.xmlcalabash.properties")
    while (uriEnum.hasMoreElements) {
      val url = uriEnum.nextElement()
      val resource = new File(url.toURI)
      val stream = new FileInputStream(resource)
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
              println(s"No namespace binding for $pfx, ignoring: $name=$value")
            }
          case SPattern(pfx,local) =>
            if (nsmap.contains(pfx)) {
              val qname = new QName(pfx, nsmap(pfx), local)
              configuration.implementAtomicStep(qname, name)
            } else {
              println(s"No namespace binding for $pfx, ignoring: $name=$value")
            }
          case _ =>
            println(s"Unparseable line: $name=$value")
        }
      }
    }

    /*
p = namespace http://www.w3.org/ns/xproc
com.xmlcalabash.functions.SystemProperty            = function p:system-property
com.xmlcalabash.steps.CastContentType               = step p:cast-content-type
     */

  }
}
