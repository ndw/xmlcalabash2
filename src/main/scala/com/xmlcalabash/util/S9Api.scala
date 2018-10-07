package com.xmlcalabash.util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.s9api.{Axis, Serializer, XdmArray, XdmAtomicValue, XdmEmptySequence, XdmMap, XdmNode, XdmNodeKind, XdmValue}
import net.sf.saxon.value.StringValue
import org.xml.sax.InputSource

import scala.collection.JavaConverters._
import scala.collection.mutable

object S9Api {
  val OPEN_BRACE = new XdmAtomicValue("{")
  val CLOSE_BRACE = new XdmAtomicValue("}")
  val OPEN_SQUARE = new XdmAtomicValue("[")
  val CLOSE_SQUARE = new XdmAtomicValue("]")
  val DOUBLE_QUOTE = new XdmAtomicValue("\"")
  val COMMA = new XdmAtomicValue(",")
  val NEWLINE = new XdmAtomicValue("\n")
  val COLON = new XdmAtomicValue(":")
  val SPACE = new XdmAtomicValue(" ")
  val NULL = new XdmAtomicValue("null")

  def documentElement(doc: XdmNode): Option[XdmNode] = {
    doc.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        val iter = doc.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val node = iter.next.asInstanceOf[XdmNode]
          if (node.getNodeKind == XdmNodeKind.ELEMENT) {
            return Some(node)
          }
        }
        None
      case XdmNodeKind.ELEMENT =>
        Some(doc)
      case _ =>
        None
    }
  }

  def inScopeNamespaces(node: XdmNode): Map[String,String] = {
    val nsiter = node.axisIterator(Axis.NAMESPACE)
    val ns = mutable.HashMap.empty[String,String]
    while (nsiter.hasNext) {
      val attr = nsiter.next().asInstanceOf[XdmNode]
      val prefix = if (attr.getNodeName == null) {
        ""
      } else {
        attr.getNodeName.toString
      }
      val uri = attr.getStringValue
      ns.put(prefix, uri)
    }
    ns.toMap
  }

  // FIXME: THIS METHOD IS A GROTESQUE HACK!
  def xdmToInputSource(config: XMLCalabash, node: XdmNode): InputSource = {
    val out = new ByteArrayOutputStream()
    val serializer = config.processor.newSerializer
    serializer.setOutputStream(out)
    serialize(config, node, serializer)
    val isource = new InputSource(new ByteArrayInputStream(out.toByteArray))
    if (node.getBaseURI != null) {
      isource.setSystemId(node.getBaseURI.toASCIIString)
    }
    isource
  }

  def valuesToString(values: XdmValue): String = {
    var str = ""
    var sep = ""
    for (pos <- 1 to values.size) {
      str = str + sep + values.itemAt(pos - 1).getStringValue
      sep = " "
    }
    str
  }

  def serialize(config: XMLCalabash, value: XdmValue, serializer: Serializer): Unit = {
    serialize(config, List(value), serializer)
  }

  def serialize(xproc: XMLCalabash, values: List[XdmValue], serializer: Serializer): Unit = {
    for (value <- values) {
      value match {
        case arr: XdmArray =>
          serializeArr(xproc, arr, serializer)
        case map: XdmMap =>
          serializeMap(xproc, map, serializer)
        case empty: XdmEmptySequence =>
          serializer.serializeXdmValue(NULL)
        case atomic: XdmAtomicValue =>
          atomic.getPrimitiveTypeName match {
            case XProcConstants.xs_string =>
              serializer.serializeXdmValue(DOUBLE_QUOTE)
              serializer.serializeXdmValue(atomic)
              serializer.serializeXdmValue(DOUBLE_QUOTE)
            case _ =>
              serializer.serializeXdmValue(atomic)
          }
        case _ => serializer.serializeXdmValue(value)
      }
    }
  }

  private def serializeMap(xproc: XMLCalabash, value: XdmMap, serializer: Serializer): Unit = {
    serializer.serializeXdmValue(OPEN_BRACE)
    val map = value.asMap()

    var first = true
    for (key <- map.asScala.keySet) {
      val value = map.asScala(key)
      if (!first) {
        serializer.serializeXdmValue(COMMA)
        serializer.serializeXdmValue(NEWLINE)
      }
      first = false

      serializer.serializeXdmValue(DOUBLE_QUOTE)
      serializer.serializeXdmValue(key)
      serializer.serializeXdmValue(DOUBLE_QUOTE)
      serializer.serializeXdmValue(COLON)
      serializer.serializeXdmValue(SPACE)
      serialize(xproc, value, serializer)
    }
    serializer.serializeXdmValue(CLOSE_BRACE)
  }

  private def serializeArr(xproc: XMLCalabash, arr: XdmArray, serializer: Serializer): Unit = {
    serializer.serializeXdmValue(OPEN_SQUARE)

    var idx = 0
    for (idx <- 0  until arr.arrayLength()) {
      val value = arr.get(idx)
      if (idx > 0) {
        serializer.serializeXdmValue(COMMA)
        serializer.serializeXdmValue(SPACE)
      }

      serialize(xproc, value, serializer)
    }
    serializer.serializeXdmValue(CLOSE_SQUARE)
  }

  /*
  def serialize(xproc: XMLCalabash, nodes: List[XdmValue], serializer: Serializer): Unit = {
    val qtproc = xproc.processor
    val xqcomp = qtproc.newXQueryCompiler
    xqcomp.setModuleURIResolver(xproc.moduleURIResolver)
    // Patch suggested by oXygen to avoid errors that result from attempting to serialize
    // a schema-valid document with a schema-naive query
    xqcomp.getUnderlyingStaticContext.setSchemaAware(xqcomp.getProcessor.getUnderlyingConfiguration.isLicensedFeature(Configuration.LicenseFeature.ENTERPRISE_XQUERY))
    val xqexec = xqcomp.compile(".")
    val xqeval = xqexec.load
    if (xproc.htmlSerializer && "html" == serializer.getOutputProperty(Serializer.Property.METHOD)) {
      var ch: ContentHandler = null
      val outputDest = serializer.getOutputDestination
      if (outputDest == null) {
        // ???
        xqeval.setDestination(serializer)
      } else {
        outputDest match {
          case out: OutputStream =>
            ch = new HtmlSerializer(out)
            xqeval.setDestination(new SAXDestination(ch))
          case out: Writer =>
            ch = new HtmlSerializer(out)
            xqeval.setDestination(new SAXDestination(ch))
          case out: File =>
            try {
              val fos = new FileOutputStream(out)
              ch = new HtmlSerializer(fos)
              xqeval.setDestination(new SAXDestination(ch))
            } catch {
              case fnfe: FileNotFoundException =>
                xqeval.setDestination(serializer)
              case t: Throwable => throw t
            }
          case _ =>
            xqeval.setDestination(serializer)
        }
      }
    } else {
      xqeval.setDestination(serializer)
    }

    for (node <- nodes) {
      xqeval.setContextItem(node)
      xqeval.run()
      // Even if we output an XML decl before the first node, we must not do it before any others!
      serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes")
    }
  }
  */
}
