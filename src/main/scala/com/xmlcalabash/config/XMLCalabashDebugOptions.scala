package com.xmlcalabash.config

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, FileOutputStream, PrintStream, PrintWriter}

import com.jafpl.graph.Graph
import com.xmlcalabash.model.xml.DeclareStep
import com.xmlcalabash.model.util.XProcConstants
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

  var dumpGraphStep = Option.empty[QName]
  var dumpGraphType = "graph"
  var dumpGraph = Option.empty[String]
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

  def dumpGraph(decl: DeclareStep): Unit = {
    if (dumpGraph.isDefined)
      dumpGraphType match {
        case "graph" =>
          graphPipeline(decl, dumpGraph.get)
        case "pipeline" =>
          dumpPipeline(decl, dumpGraph.get)
        case _ => Unit
    }
  }

  def dumpGraph(graph: Graph, open: Boolean): Unit = {
    if (dumpGraph.isEmpty) {
      return
    }

    if (open) {
      if (dumpGraphType == "opengraph") {
        graphGraph(graph, dumpGraph.get + graph.uid.toString)
      }
    } else {
      if (dumpGraphType == "jafpl") {
        graphGraph(graph, dumpGraph.get + graph.uid.toString)
      }
    }
  }

  private def dumpPipeline(decl: DeclareStep, baseName: String): Unit = {
    val fn = if (baseName.contains(".")) {
      baseName
    } else {
      baseName + ".xml"
    }

    val fos = new FileOutputStream(new File(fn))
    var pw = new PrintWriter(fos)
    pw.write(decl.xdump.toString)
    pw.close()
    fos.close()
  }

  private def graphGraph(graph: Graph, baseName: String): Unit = {
    val fn = if (baseName.contains(".")) {
      baseName
    } else {
      baseName + ".svg"
    }

    val baos = new ByteArrayOutputStream()
    var pw = new PrintWriter(baos)
    pw.write(graph.asXML.toString)
    pw.close()
    svgGraph(fn, baos)
  }

  private def graphPipeline(decl: DeclareStep, baseName: String): Unit = {
    val fn = if (baseName.contains(".")) {
      baseName
    } else {
      baseName + ".svg"
    }

    val baos = new ByteArrayOutputStream()
    var pw = new PrintWriter(baos)
    pw.write(decl.xdump.toString)
    pw.close()
    svgPipeline(fn, baos)
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

  private def svgPipeline(fn: String, xmlBaos: ByteArrayOutputStream): Unit = {
    if (config.debugOptions.graphviz_dot.isEmpty) {
      logger.error(s"GraphViz dot not configured, cannot dump $fn.")
      return
    }

    // FIXME: Make this into a pipeline!

    val processor = config.processor

    // Get the source node
    val bais = new ByteArrayInputStream(xmlBaos.toByteArray)
    val builder = processor.newDocumentBuilder()
    val graphdoc = builder.build(new SAXSource(new InputSource(bais)))

    // Get the first stylesheet
    var stylesheet = getClass.getResourceAsStream("/com/xmlcalabash/stylesheets/pl2dot.xsl")
    var compiler = processor.newXsltCompiler()
    compiler.setSchemaAware(processor.isSchemaAware)
    var exec = compiler.compile(new SAXSource(new InputSource(stylesheet)))

    // Transform to dot: XML
    var transformer = exec.load()
    transformer.setInitialContextNode(graphdoc)
    var result = new XdmDestination()
    transformer.setDestination(result)
    transformer.transform()
    var xformed = result.getXdmNode

    // Get the second stylesheet
    stylesheet = getClass.getResourceAsStream("/com/xmlcalabash/stylesheets/dot2txt.xsl")
    compiler = processor.newXsltCompiler()
    compiler.setSchemaAware(processor.isSchemaAware)
    exec = compiler.compile(new SAXSource(new InputSource(stylesheet)))

    // Transform to dot
    transformer = exec.load()
    transformer.setInitialContextNode(xformed)
    result = new XdmDestination()
    transformer.setDestination(result)
    transformer.transform()
    xformed = result.getXdmNode

    // Write the DOT file to a temp file
    val temp = File.createTempFile("calabash", ".dot")
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

  /*
  def dumpRaw(graph: Graph, decl: DeclareStep): Unit = {
    if (!dumpRaw) {
      return
    }

    val fn = baseName(decl, cx_raw, ".xml")
    logger.debug("Dumping raw graph of " + decl.stepName + " to: " + fn)

    val stdout = new FileOutputStream(new File(fn))
    val psout = new PrintStream(stdout)
    Console.withOut(psout) {
      graph.dump()
    }
  }
   */
}
