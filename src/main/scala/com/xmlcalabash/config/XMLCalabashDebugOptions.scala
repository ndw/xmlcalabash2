package com.xmlcalabash.config

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, FileOutputStream, PrintStream, PrintWriter}

import com.jafpl.graph.Graph
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.DeclareStep
import javax.xml.transform.sax.SAXSource
import net.sf.saxon.s9api.{QName, XdmDestination}
import org.slf4j.{Logger, LoggerFactory}
import org.xml.sax.InputSource

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

protected class XMLCalabashDebugOptions(config: XMLCalabashConfig) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val cx_graph = new QName(XProcConstants.ns_cx, "graph")
  private val cx_open_graph = new QName(XProcConstants.ns_cx, "open-graph")
  private val cx_xml = new QName(XProcConstants.ns_cx, "xml")
  private val cx_raw = new QName(XProcConstants.ns_cx, "raw")
  private val filenames = mutable.HashSet.empty[String]
  private val _injectables = ListBuffer.empty[String]

  var dumpXml = false
  var dumpOpenGraph = false
  var dumpGraph = false
  var dumpRaw = false
  var norun: Boolean = false
  var debug: Boolean = false
  var _graphviz_dot = Option.empty[String]

  def injectables: List[String] = _injectables.toList
  def injectables_=(list: List[String]): Unit = {
    _injectables.clear()
    _injectables ++= list
  }

  def graphviz_dot: Option[String] = _graphviz_dot
  def graphviz_dot_=(dot: String): Unit = {
    _graphviz_dot = Some(dot)
  }

  // ===========================================================================================

  private def baseName(decl: DeclareStep, kind: QName, ext: String): String = {
    val dumpFn = decl.extensionAttribute(kind)
    val baseName = if (dumpFn.isDefined) {
      dumpFn.get
    } else {
      var fn = if (decl.name.startsWith("!")) {
        decl.name.substring(1).replace(".", "_")
      } else {
        decl.name
      }

      if (filenames.contains(fn)) {
        fn + "-" + decl.hashCode().toHexString
      } else {
        filenames += fn
        fn
      }
    }

    if (baseName.endsWith(ext)) {
      baseName
    } else {
      baseName + ext
    }
  }

  def dumpGraph(graph: Graph, decl: DeclareStep): Unit = {
    if (!dumpGraph) {
      return
    }

    val fn = baseName(decl, cx_graph, ".svg")
    logger.debug("Dumping graph of " + decl.name + " to: " + fn)

    val baos = new ByteArrayOutputStream()
    var pw = new PrintWriter(baos)
    pw.write(graph.asXML.toString)
    pw.close()

    svgGraph(fn, baos)
  }

  def dumpOpenGraph(graph: Graph, decl: DeclareStep): Unit = {
    if (!dumpOpenGraph) {
      return
    }

    val fn = baseName(decl, cx_open_graph, ".svg")
    logger.debug("Dumping open graph of " + decl.name + " to: " + fn)

    val baos = new ByteArrayOutputStream()
    var pw = new PrintWriter(baos)
    pw.write(graph.asXML.toString)
    pw.close()

    svgGraph(fn, baos)
  }

  private def svgGraph(fn: String, xmlBaos: ByteArrayOutputStream): Unit = {
    if (config.debugOptions.graphviz_dot.isEmpty) {
      logger.error(s"GraphViz dot not configured, cannot dump $fn.")
      return
    }

    val processor = config.processor

    // Get the source node
    val bais = new ByteArrayInputStream(xmlBaos.toByteArray)
    val builder = processor.newDocumentBuilder()
    val graphdoc = builder.build(new SAXSource(new InputSource(bais)))

    // Get the stylesheet
    val stylesheet = getClass.getResourceAsStream("/com/jafpl/stylesheets/pg2dot.xsl")
    val compiler = processor.newXsltCompiler()
    compiler.setSchemaAware(processor.isSchemaAware)
    val exec = compiler.compile(new SAXSource(new InputSource(stylesheet)))

    // Transform to DOT
    val transformer = exec.load()
    transformer.setInitialContextNode(graphdoc)
    val result = new XdmDestination()
    transformer.setDestination(result)
    transformer.transform()
    val xformed = result.getXdmNode

    // Write the DOT file to a temp file
    val temp = File.createTempFile("calabash", "dot")
    temp.deleteOnExit()
    val dot = new FileOutputStream(temp)
    val pw = new PrintWriter(dot)
    pw.println(xformed.getStringValue)
    pw.close()

    // Transform it into SVG
    val rt = Runtime.getRuntime
    val args = Array("/usr/local/bin/dot", "-Tsvg", temp.getAbsolutePath, "-o", fn)
    val p = rt.exec(args)
    p.waitFor()

    temp.delete()
  }

  def dumpXml(decl: DeclareStep): Unit = {
    if (!dumpXml) {
      return
    }

    val fn = baseName(decl, cx_xml, ".xml")
    logger.debug("Dumping XML of " + decl.name + " to: " + fn)
    val pw = new PrintWriter(new File(fn))
    pw.write(decl.asXML.toString)
    pw.close()
  }

  def dumpRaw(graph: Graph, decl: DeclareStep): Unit = {
    if (!dumpRaw) {
      return
    }

    val fn = baseName(decl, cx_raw, ".xml")
    logger.debug("Dumping raw graph of " + decl.name + " to: " + fn)

    val stdout = new FileOutputStream(new File(fn))
    val psout = new PrintStream(stdout)
    Console.withOut(psout) {
      graph.dump()
    }
  }
}
