package com.xmlcalabash.testers

import java.io.{ByteArrayOutputStream, File, PrintStream}
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.{Calendar, GregorianCalendar}
import javax.xml.transform.sax.SAXSource

import com.jafpl.messages.{ItemMessage, Message}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.TestException
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser}
import com.xmlcalabash.runtime.{ExpressionContext, NodeLocation, SaxonExpressionEvaluator, XProcMetadata, XProcXPathExpression}
import com.xmlcalabash.util.{S9Api, URIUtils}
import net.sf.saxon.s9api.{Axis, QName, XdmAtomicValue, XdmItem, XdmNode, XdmNodeKind}
import org.slf4j.{Logger, LoggerFactory}
import org.xml.sax.InputSource

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class TestRunner(runtimeConfig: XMLCalabash, testloc: String) {
  private val _testsuite = new QName("", "testsuite")
  private val _properties = new QName("", "properties")
  private val _property = new QName("", "property")
  private val _value = new QName("", "value")
  private val _testcase = new QName("", "testcase")
  private val _classname = new QName("", "classname")
  private val _time = new QName("", "time")
  private val _timestamp = new QName("", "timestamp")
  private val _system_out = new QName("", "system-out")
  private val _system_err = new QName("", "system-err")
  private val _hostname = new QName("", "hostname")
  private val _tests = new QName("", "tests")
  private val _error = new QName("", "error")
  private val _failure = new QName("", "failure")
  private val _failures = new QName("", "failures")
  private val _message = new QName("", "message")
  private val _type = new QName("", "type")

  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val testFiles = ListBuffer.empty[String]
  private val dir = new File(testloc)
  private val fnregex = "^.*.xml".r
  private val tsns = "http://xproc.org/ns/testsuite/3.0"
  private val t_test_suite = new QName(tsns, "test-suite")
  private val t_div = new QName(tsns, "div")
  private val t_title = new QName(tsns, "title")
  private val t_test = new QName(tsns, "test")
  private val t_pipeline = new QName(tsns, "pipeline")
  private val t_schematron = new QName(tsns, "schematron")
  private val t_input = new QName(tsns, "input")
  private val t_option = new QName(tsns, "option")
  private val _src = new QName("", "src")
  private val _port = new QName("", "port")
  private val _name = new QName("", "name")
  private val _select = new QName("", "select")
  private val _expected = new QName("", "expected")
  private val _code = new QName("", "code")
  private val _when = new QName("", "when")

  private val processor = runtimeConfig.processor
  private val builder = processor.newDocumentBuilder()
  builder.setDTDValidation(false)
  builder.setLineNumbering(true)

  if (dir.exists) {
    if (dir.isDirectory) {
      recurse(dir)
    } else {
      testFiles += testloc
    }
  }

  if (testFiles.isEmpty) {
    throw new TestException(s"Test runner cannot find tests at: $testloc")
  }

  def run(): Option[String] = {
    var firstError = Option.empty[String]

    for (fn <- testFiles) {
      val source = new SAXSource(new InputSource(fn))
      val node = builder.build(source)
      val result = runTestDocument(node)
      if (result.isDefined && firstError.isEmpty) {
        firstError = result
      }
    }

    firstError
  }

  def junit(): XdmNode = {
    var firstError = Option.empty[String]

    val junit = new SaxonTreeBuilder(runtimeConfig)
    junit.startDocument(URIUtils.cwdAsURI)

    var count = 0
    var failures = 0
    for (fn <- testFiles) {
      count += 1
      logger.info(s"Running $count of ${testFiles.length}: $fn")

      val stdout = new ByteArrayOutputStream()
      val psout = new PrintStream(stdout)

      val stderr = new ByteArrayOutputStream()
      val pserr = new PrintStream(stderr)

      junit.addStartElement(_testcase)
      junit.addAttribute(_name, fn)
      junit.addAttribute(_classname, "NA")

      Console.withOut(psout) {
        Console.withErr(pserr) {
          val source = new SAXSource(new InputSource(fn))
          val node = builder.build(source)

          var result: Option[String] = None
          var error: Throwable = null

          val start_ms = Calendar.getInstance().getTimeInMillis
          try {
            result = runTestDocument(node)
          } catch {
            case t: Throwable =>
              result = Some("ERROR")
              error = t
          }
          val end_ms = Calendar.getInstance().getTimeInMillis
          junit.addAttribute(_time, ((end_ms - start_ms) / 1000.0).toString)
          junit.startContent()

          if (result.isDefined) {
            failures += 1
            logger.info(s"**** FAIL **** $failures **** $result")

            if (firstError.isEmpty) {
              firstError = result
            }

            if (Option(error).isDefined) {
              junit.addStartElement(_error)
              junit.addAttribute(_message, error.getMessage)
              junit.addAttribute(_type, error.getClass.getName)
              junit.startContent()

              val stderr2 = new ByteArrayOutputStream()
              val pstrace = new PrintStream(stderr2)
              Console.withErr(pstrace) {
                error.printStackTrace(Console.err)
              }

              junit.addText(stderr2.toString)
              junit.addEndElement()

            } else {
              junit.addStartElement(_failure)
              junit.startContent()
              junit.addText(result.get)
              junit.addEndElement()
            }
          }
        }
      }

      junit.addStartElement(_system_out)
      junit.startContent()
      junit.addText(stdout.toString())
      junit.addEndElement()

      junit.addStartElement(_system_err)
      junit.startContent()
      junit.addText(stderr.toString())
      junit.addEndElement()

      junit.addEndElement()
    }

    logger.info(s"$failures of $count tests failed")

    val wrapper = new SaxonTreeBuilder(runtimeConfig)
    wrapper.startDocument(URIUtils.cwdAsURI)
    wrapper.addStartElement(_testsuite)
    wrapper.addAttribute(_name, "NA")

    val today   = Calendar.getInstance().getTime
    val dformat = new SimpleDateFormat("YYYY-MM-dd")
    val tformat = new SimpleDateFormat("HH:mm:ss")
    var stamp   = dformat.format(today) + "T" + tformat.format(today)
    wrapper.addAttribute(_timestamp, stamp)

    wrapper.addAttribute(_hostname, InetAddress.getLocalHost.getHostName)
    wrapper.addAttribute(_tests, count.toString)
    wrapper.addAttribute(_failures, failures.toString)

    wrapper.startContent()
    wrapper.addStartElement(_properties)
    wrapper.startContent()

    wrapper.addStartElement(_property)
    wrapper.addAttribute(_name, "processor")
    wrapper.addAttribute(_value, runtimeConfig.productName)
    wrapper.startContent()
    wrapper.addEndElement()

    wrapper.addStartElement(_property)
    wrapper.addAttribute(_name, "version")
    wrapper.addAttribute(_value, runtimeConfig.productVersion)
    wrapper.startContent()
    wrapper.addEndElement()
    wrapper.addEndElement()

    wrapper.addSubtree(junit.result)

    wrapper.addEndElement()
    wrapper.endDocument()
    wrapper.result
  }

  private def runTestDocument(node: XdmNode): Option[String] = {
    var firstError = Option.empty[String]

    if (node.getNodeKind != XdmNodeKind.DOCUMENT) {
      throw new TestException("Unexpected node type in runTestSuite(): " + node.getNodeKind)
    }

    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next().asInstanceOf[XdmNode]
      child.getNodeKind match {
        case XdmNodeKind.ELEMENT =>
          if (child.getNodeName == t_test_suite) {
            val result = runTestSuite(child)
            if (result.isDefined && firstError.isEmpty) {
              firstError = result
            }
          } else if (child.getNodeName == t_test) {
            val result = runTest(child)
            if (result.isDefined && firstError.isEmpty) {
              firstError = result
            }
          } else {
            throw new TestException(s"Unexpected element ${child.getNodeName} in ${child.getBaseURI}")
          }
        case XdmNodeKind.TEXT =>
          if (node.getStringValue.trim != "") {
            throw new TestException(s"Unexpected text ${node.getStringValue} in ${child.getBaseURI}")
          }
        case _ => Unit
      }
    }

    firstError
  }

  private def runTestSuite(node: XdmNode): Option[String] = {
    var firstError = Option.empty[String]

    if ((node.getNodeKind != XdmNodeKind.ELEMENT) || (node.getNodeName != t_test_suite)) {
      throw new TestException("Unexpected node in runTestSuite(): " + node.getNodeKind)
    }

    val when = node.getAttributeValue(_when)
    if (when != null) {
      val evaluator = new SaxonExpressionEvaluator(runtimeConfig)
      val expr = new XProcXPathExpression(ExpressionContext.NONE, when)
      val run = evaluator.booleanValue(expr, List.empty[Message], Map.empty[String,Message], None)
      if (!run) {
        logger.info("Skipping test-suite")
        return None // FIXME: work out some way to communicate that a test was skipped
      }
    }

    val src = node.getAttributeValue(_src)
    if (src != null) {
      val suite = loadResource(node)
      if (suite.isDefined) {
        return runTestSuite(S9Api.documentElement(suite.get).get)
      } else {
        throw new TestException(s"Failed to load test-suite: $src")
      }
    }

    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next().asInstanceOf[XdmNode]
      child.getNodeKind match {
        case XdmNodeKind.ELEMENT =>
          if (child.getNodeName == t_div) {
            val result = runTestDiv(child)
            if (result.isDefined && firstError.isEmpty) {
              firstError = result
            }
          } else if (child.getNodeName == t_test) {
            val result = runTest(child)
            if (result.isDefined && firstError.isEmpty) {
              firstError = result
            }
          } else if (child.getNodeName == t_title) {
            Unit
          } else {
            throw new TestException(s"Unexpected element ${child.getNodeName} in ${child.getBaseURI}")
          }
        case XdmNodeKind.TEXT =>
          if (node.getStringValue.trim != "") {
            throw new TestException(s"Unexpected text ${node.getStringValue} in ${child.getBaseURI}")
          }
        case _ => Unit
      }
    }

    firstError
  }

  private def runTestDiv(node: XdmNode): Option[String] = {
    var firstError = Option.empty[String]

    if ((node.getNodeKind != XdmNodeKind.ELEMENT) || (node.getNodeName != t_div)) {
      throw new TestException("Unexpected node in runTestSuite(): " + node.getNodeKind)
    }

    val when = node.getAttributeValue(_when)
    if (when != null) {
      val evaluator = new SaxonExpressionEvaluator(runtimeConfig)
      val expr = new XProcXPathExpression(ExpressionContext.NONE, when)
      val run = evaluator.booleanValue(expr, List.empty[Message], Map.empty[String,Message], None)
      if (!run) {
        logger.info("Skipping test-div")
        return None // FIXME: work out some way to communicate that a test was skipped
      }
    }

    val src = node.getAttributeValue(_src)
    if (src != null) {
      val divdoc = loadResource(node)
      if (divdoc.isDefined) {
        return runTestDiv(S9Api.documentElement(divdoc.get).get)
      } else {
        throw new TestException(s"Failed to load test div: $src")
      }
    }

    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next().asInstanceOf[XdmNode]
      child.getNodeKind match {
        case XdmNodeKind.ELEMENT =>
          if (child.getNodeName == t_test) {
            val result = runTest(child)
            if (result.isDefined && firstError.isEmpty) {
              firstError = result
            }
          } else if (child.getNodeName == t_title) {
            Unit
          } else {
            throw new TestException(s"Unexpected element ${child.getNodeName} in ${child.getBaseURI}")
          }
        case XdmNodeKind.TEXT =>
          if (node.getStringValue.trim != "") {
            throw new TestException(s"Unexpected text ${node.getStringValue} in ${child.getBaseURI}")
          }
        case _ => Unit
      }
    }

    firstError
  }

  private def runTest(node: XdmNode): Option[String] = {
    if ((node.getNodeKind != XdmNodeKind.ELEMENT) || (node.getNodeName != t_test)) {
      throw new TestException("Unexpected node in runTestSuite(): " + node.getNodeKind)
    }

    val when = node.getAttributeValue(_when)
    if (when != null) {
      val evaluator = new SaxonExpressionEvaluator(runtimeConfig)
      val expr = new XProcXPathExpression(ExpressionContext.NONE, when)
      val run = evaluator.booleanValue(expr, List.empty[Message], Map.empty[String,Message], None)
      if (!run) {
        logger.info("Skipping test")
        return None // FIXME: work out some way to communicate that a test was skipped
      }
    }

    val src = node.getAttributeValue(_src)
    if (src != null) {
      val testdoc = loadResource(node)
      if (testdoc.isDefined) {
        return runTest(S9Api.documentElement(testdoc.get).get)
      } else {
        throw new TestException(s"Failed to load test div: $src")
      }
    }

    val expected = node.getAttributeValue(_expected)
    if ((expected != "pass") && (expected != "fail")) {
      throw new TestException("Test expectation unspecified")
    }

    var pipeline = Option.empty[XdmNode]
    var schematron = Option.empty[XdmNode]
    var inputs = mutable.HashMap.empty[String, XdmNode]
    var bindings = mutable.HashMap.empty[QName, XdmItem]

    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next().asInstanceOf[XdmNode]
      child.getNodeKind match {
        case XdmNodeKind.ELEMENT =>
          if (child.getNodeName == t_pipeline) {
            if (pipeline.isDefined) {
              throw new TestException("Pipeline is already defined")
            }
            pipeline = loadResource(child)
          } else if (child.getNodeName == t_schematron) {
            if (schematron.isDefined) {
              throw new TestException("Schematron is already defined")
            }
            schematron = loadResource(child)
          } else if (child.getNodeName == t_input) {
            val port = child.getAttributeValue(_port)
            if (port == null) {
              throw new TestException("Input has no port")
            }
            if (inputs.contains(port)) {
              throw new TestException(s"Input $port is already defined")
            }
            val doc = loadResource(child)
            if (doc.isEmpty) {
              throw new TestException(s"Failed to load input for $port")
            }
            inputs.put(port, doc.get)
          } else if (child.getNodeName == t_option) {
            val name = child.getAttributeValue(_name)
            if (name == null) {
              throw new TestException("Option has no name")
            }
            // FIXME: what about qnames?
            val qname = new QName("", name)
            if (bindings.contains(qname)) {
              throw new TestException(s"Binding $name is already defined")
            }
            val value = loadBinding(child)
            if (value.isEmpty) {
              throw new TestException(s"Failed to load binding for $name")
            }
            bindings.put(qname, value.get)
          } else {
            throw new TestException(s"Unexpected element ${child.getNodeName} in ${child.getBaseURI}")
          }

        case XdmNodeKind.TEXT =>
          if (child.getStringValue.trim != "") {
            throw new TestException(s"Unexpected text ${child.getStringValue} in ${child.getBaseURI}")
          }
        case _ => Unit
      }
    }

    val tester = new Tester(runtimeConfig)

    if (pipeline.isEmpty) {
      throw new TestException("No pipeline for test")
    }

    tester.pipeline = pipeline.get

    if (schematron.isEmpty) {
      if (expected == "pass") {
        logger.warn("No schematron for test result.")
      }
    } else {
      tester.schematron = schematron.get
    }

    for ((port,doc) <- inputs) {
      tester.addInput(port,doc)
    }

    for ((name,bind) <- bindings) {
      tester.addBinding(name, bind)
    }

    val result = tester.run()

    if (result.isEmpty) {
      if (expected == "pass") {
        result
      } else {
        Some("notfailed")
      }
    } else {
      val code = node.getAttributeValue(_code)
      if (code == null) {
        Some(s"null != ${result.get}")
      } else {
        val qcode = ValueParser.parseQName(code, S9Api.inScopeNamespaces(node), Some(new NodeLocation(node)))
        if (qcode.getClarkName == result.get) {
          None
        } else {
          Some(s"${qcode.getClarkName} != ${result.get}")
        }
      }
    }
  }

  private def loadResource(node: XdmNode): Option[XdmNode] = {
    val children = ListBuffer.empty[XdmNode]
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      children += iter.next.asInstanceOf[XdmNode]
    }

    val src = node.getAttributeValue(_src)

    if (src == null) {
      inlineDocument(node)
    } else {
      if (children.nonEmpty) {
        throw new TestException(s"If you specify @src, the ${node.getNodeName} must be empty")
      }

      val docsrc = new SAXSource(new InputSource(node.getBaseURI.resolve(src).toASCIIString))
      val doc = builder.build(docsrc)
      Some(doc)
    }
  }

  private def loadBinding(node: XdmNode): Option[XdmItem] = {
    val children = ListBuffer.empty[XdmNode]
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      children += iter.next.asInstanceOf[XdmNode]
    }

    val src = node.getAttributeValue(_src)
    if ((src == null) && children.isEmpty) {
      val exprContext = new ExpressionContext(node.getBaseURI, S9Api.inScopeNamespaces(node), new NodeLocation(node))
      val value = node.getAttributeValue(_select)
      val eval = runtimeConfig.expressionEvaluator
      val context = inlineDocument(node)
      val message = new ItemMessage(context.get, new XProcMetadata("application/xml"))
      val result = eval.singletonValue(new XProcXPathExpression(exprContext, value), List(message), Map.empty[String,Message], None)
      result match {
        case item: XPathItemMessage =>
          Some(item.item)
        case item: ItemMessage =>
          Some(item.item.asInstanceOf[XdmItem])
        case _ =>
          logger.warn("Unexpected option result: " + result)
          Some(new XdmAtomicValue(result.toString))
      }
    } else {
      loadResource(node)
    }
  }

  private def inlineDocument(node: XdmNode): Option[XdmNode] = {
    val builder = new SaxonTreeBuilder(runtimeConfig)
    val iter = node.axisIterator(Axis.CHILD)

    builder.startDocument(node.getBaseURI)
    while (iter.hasNext) {
      builder.addSubtree(iter.next().asInstanceOf[XdmNode])
    }
    builder.endDocument()

    val rnode = builder.result
    Some(rnode)
  }

  private def recurse(dir: File): Unit = {
    for (file <- dir.listFiles()) {
      if (file.isDirectory) {
        recurse(file)
      } else {
        file.getName match {
          case fnregex() =>
            testFiles += file.getAbsolutePath
          case _ => Unit
        }
      }
    }
  }
}
