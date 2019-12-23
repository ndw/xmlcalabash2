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
  private val STACKTRACE = "stacktrace"
  private val TREE = "tree"
  private val XMLTREE = "xml-tree"
  private val GRAPH = "graph"
  private val JAFPLGRAPH = "jafpl-graph"
  private val OPENGRAPH = "open-graph"

  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val _injectables = ListBuffer.empty[String]
  private var _graphviz_dot = Option.empty[String]
  private var _run = true

  private var _output_directory = "."
  private var _stack_trace = Option.empty[String]
  private var _tree = Option.empty[String]
  private var _xml_tree = Option.empty[String]
  private var _graph = Option.empty[String]
  private var _jafpl_graph = Option.empty[String]
  private var _open_graph = Option.empty[String]

  private var debugOptions = mutable.HashSet.empty[String]
  private val dumped = mutable.HashMap.empty[DeclareStep,mutable.HashSet[String]]
  private val dumpCount = mutable.HashMap.empty[DeclareStep,mutable.HashMap[String,Long]]

  def injectables: List[String] = _injectables.toList
  def injectables_=(list: List[String]): Unit = {
    _injectables.clear()
    _injectables ++= list
  }

  def graphviz_dot: Option[String] = _graphviz_dot
  def graphviz_dot_=(dot: String): Unit = {
    _graphviz_dot = Some(dot)
  }

  def run: Boolean = _run
  def run_=(run: Boolean): Unit = {
    _run = run
  }

  def outputDirectory: String = _output_directory
  def outputDirectory_=(dir: String): Unit = {
    _output_directory = dir
  }

  def stackTrace: Option[String] = _stack_trace
  def stackTrace_=(trace: Option[String]): Unit = {
    _stack_trace = trace
    debugOptions += STACKTRACE
  }

  def tree: Option[String] = _tree
  def tree_=(tree: Option[String]): Unit = {
    _tree = tree
    debugOptions += TREE
  }

  def xmlTree: Option[String] = _xml_tree
  def xmlTree_=(xml_tree: Option[String]): Unit = {
    _xml_tree = xml_tree
    debugOptions += XMLTREE
  }

  def graph: Option[String] = _graph
  def graph_=(graph: Option[String]): Unit = {
    _graph = graph
    debugOptions += GRAPH
  }

  def jafplGraph: Option[String] = _jafpl_graph
  def jafplGraph_=(graph: Option[String]): Unit = {
    _jafpl_graph = graph
    debugOptions += JAFPLGRAPH
  }

  def openGraph: Option[String] = _open_graph
  def openGraph_=(graph: Option[String]): Unit = {
    _open_graph = graph
    debugOptions += OPENGRAPH
  }

  // ===========================================================================================

  def dumpStacktrace(decl: Option[DeclareStep], exception: Exception): Unit = {
    if (decl.isDefined) {
      dump(decl.get, STACKTRACE, None, Some(exception))
    } else {
      dumpStackTrace(exception)
    }
  }

  private def dumpStackTrace(exception: Exception): Unit = {
    val opt = STACKTRACE

    if (!debugOptions.contains(opt)) {
      return
    }

    if (stackTrace.isDefined) {
      val basefn = stackTrace.get
      val fn = s"$outputDirectory/$basefn.txt"
      val fos = new FileOutputStream(new File(fn))
      val pos = new PrintStream(fos)
      exception.printStackTrace(pos)
      pos.close()
    } else {
      exception.printStackTrace()
    }
  }

  def dumpTree(decl: DeclareStep): Unit = {
    dump(decl, TREE)
  }

  def dumpXmlTree(decl: DeclareStep): Unit = {
    dump(decl, XMLTREE)
  }

  def dumpGraph(decl: DeclareStep): Unit = {
    dump(decl, GRAPH)
  }

  def dumpJafplGraph(decl: DeclareStep, graph: Graph): Unit = {
    dump(decl, JAFPLGRAPH, Some(graph), None)
  }

  def dumpOpenGraph(decl: DeclareStep, graph: Graph): Unit = {
    dump(decl, OPENGRAPH, Some(graph), None)
  }

  private def dump(decl: DeclareStep, opt: String): Unit = {
    dump(decl, opt, None, None)
  }

  private def dump(decl: DeclareStep, opt: String, graph: Option[Graph], exception: Option[Exception]): Unit = {
    /*
    if (!debugOptions.contains(opt)) {
      return
    }
     */

    val output = dumped.getOrElse(decl, mutable.HashSet.empty[String])
    if (output.contains(opt)) {
      return
    }
    output += opt
    dumped.put(decl, output)

    val name = if (decl.stepName.startsWith("!")) {
      decl.stepName.substring(1).replace(".","_")
    } else {
      decl.stepName
    }

    val counts = dumpCount.getOrElse(decl, mutable.HashMap.empty[String,Long])
    val count = counts.getOrElse(opt, 0L)

    var ext = ""
    if (count > 0) {
      ext = s"$count%03d"
    }

    counts.put(opt,count+1)

    opt match {
      case TREE =>
        if (tree.isDefined) {
          val basefn = tree.getOrElse(name)
          val fn = s"$outputDirectory/$basefn$ext.txt"
          val fos = new FileOutputStream(new File(fn))
          val psout = new PrintStream(fos)
          Console.withOut(psout) {
            decl.dump()
          }
          psout.close()
        } else {
          Console.withOut(System.err) {
            decl.dump()
          }
        }
      case XMLTREE =>
        val basefn = xmlTree.getOrElse(name)
        val fn = s"$outputDirectory/$basefn$ext.xml"
        val fos = new FileOutputStream(new File(fn))
        val pw = new PrintWriter(fos)
        pw.write(decl.xdump.toString)
        pw.close()
        fos.close()
      case GRAPH =>
        val basefn = graph.getOrElse(name)
        val fn = s"$outputDirectory/$basefn$ext.svg"
        val baos = new ByteArrayOutputStream()
        val pw = new PrintWriter(baos)
        pw.write(decl.xdump.toString)
        pw.close()
        svgPipeline(fn, baos)
      case JAFPLGRAPH =>
        val basefn = jafplGraph.getOrElse(name)
        if (debugOptions.contains(GRAPH) && graph.isEmpty
          || debugOptions.contains(OPENGRAPH) && openGraph.isEmpty) {
          ext = s"_jafpl$ext"
        }
        val fn = s"$outputDirectory/$basefn$ext.svg"
        val baos = new ByteArrayOutputStream()
        val pw = new PrintWriter(baos)
        pw.write(graph.get.asXML.toString)
        //val xxx = graph.get.asXML.toString()
        pw.close()
        svgGraph(fn, baos, "pgx2dot.xsl")
      case OPENGRAPH =>
        val basefn = openGraph.getOrElse(name)
        if (debugOptions.contains(GRAPH) && graph.isEmpty
          || debugOptions.contains(JAFPLGRAPH) && jafplGraph.isEmpty) {
          ext = s"_open$ext"
        }
        val fn = s"$outputDirectory/$basefn$ext.svg"
        val baos = new ByteArrayOutputStream()
        val pw = new PrintWriter(baos)
        pw.write(graph.get.asXML.toString)
        //val xxx = graph.get.asXML.toString()
        pw.close()
        svgGraph(fn, baos, "pg2dot.xsl")
      case STACKTRACE =>
        if (stackTrace.isDefined) {
          val basefn = stackTrace.get
          val fn = s"$outputDirectory/$basefn$ext.txt"
          val fos = new FileOutputStream(new File(fn))
          val pos = new PrintStream(fos)
          exception.get.printStackTrace(pos)
          pos.close()
        } else {
          exception.get.printStackTrace()
        }
    }
  }

  private def svgGraph(fn: String, xmlBaos: ByteArrayOutputStream, style: String): Unit = {
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
    val stylesheet = getClass.getResourceAsStream("/com/jafpl/stylesheets/" + style)
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
