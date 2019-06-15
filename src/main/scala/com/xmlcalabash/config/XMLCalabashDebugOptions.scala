package com.xmlcalabash.config

import java.io.{File, FileOutputStream, PrintStream, PrintWriter}

import com.jafpl.graph.Graph
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.DeclareStep
import net.sf.saxon.s9api.QName
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ListBuffer

class XMLCalabashDebugOptions {
  private val cx_dump = new QName(XProcConstants.ns_cx, "dump")
  private val cx_dump_open = new QName(XProcConstants.ns_cx, "dump-open")
  private val cx_dump_xml = new QName(XProcConstants.ns_cx, "dump-xml")
  private val cx_dump_raw = new QName(XProcConstants.ns_cx, "dump-raw")

  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val _injectables = ListBuffer.empty[String]

  var dumpXml = false
  var dumpOpenGraph = false
  var dumpGraph = false
  var dumpRaw = false
  var norun: Boolean = false
  var debug: Boolean = false

  def injectables: List[String] = _injectables.toList
  def injectables_=(list: List[String]): Unit = {
    _injectables.clear()
    _injectables ++= list
  }

  // ===========================================================================================

  private def baseName(decl: DeclareStep, extn: QName): String = {
    val dumpFn = decl.extensionAttribute(extn)
    val baseName = if (dumpFn.isDefined) {
      dumpFn.get
    } else {
      var instanceName = decl.toString()
      val pos = instanceName.lastIndexOf(".")
      instanceName = instanceName.substring(pos+1, instanceName.length-1)
      instanceName.replace("@", "-") + "-" + extn.getLocalName
    }

    if (baseName.endsWith(".xml")) {
      baseName
    } else {
      baseName + ".xml"
    }
  }

  def dumpGraph(graph: Graph, decl: DeclareStep): Unit = {
    if (!dumpGraph) {
      return
    }

    val fn = baseName(decl, cx_dump)
    logger.debug("Dumping graph of " + decl.name + " to: " + fn)
    val pw = new PrintWriter(new File(fn))
    pw.write(graph.asXML.toString)
    pw.close()
  }

  def dumpOpenGraph(graph: Graph, decl: DeclareStep): Unit = {
    if (!dumpOpenGraph) {
      return
    }

    val fn = baseName(decl, cx_dump_open)
    logger.debug("Dumping open graph of " + decl.name + " to: " + fn)
    val pw = new PrintWriter(new File(fn))
    pw.write(graph.asXML.toString)
    pw.close()
  }

  def dumpXml(decl: DeclareStep): Unit = {
    if (!dumpXml) {
      return
    }

    val fn = baseName(decl, cx_dump_xml)
    logger.debug("Dumping XML of " + decl.name + " to: " + fn)
    val pw = new PrintWriter(new File(fn))
    pw.write(decl.asXML.toString)
    pw.close()
  }

  def dumpRaw(graph: Graph, decl: DeclareStep): Unit = {
    if (!dumpRaw) {
      return
    }

    val fn = baseName(decl, cx_dump_raw)
    logger.debug("Dumping raw graph of " + decl.name + " to: " + fn)

    val stdout = new FileOutputStream(new File(fn))
    val psout = new PrintStream(stdout)
    Console.withOut(psout) {
      graph.dump()
    }
  }
}
