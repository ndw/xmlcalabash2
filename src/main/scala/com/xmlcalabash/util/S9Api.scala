package com.xmlcalabash.util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.URI

import com.jafpl.graph.Location
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XMLCalabashRuntime, XProcMetadata}
import net.sf.saxon.`type`.BuiltInAtomicType
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.om.InscopeNamespaceResolver
import net.sf.saxon.s9api.{Axis, QName, Serializer, XdmArray, XdmAtomicValue, XdmEmptySequence, XdmItem, XdmMap, XdmNode, XdmNodeKind, XdmValue}
import net.sf.saxon.value.QNameValue
import org.xml.sax.InputSource

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

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
          val node = iter.next
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
      val attr = nsiter.next()
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
  def xdmToInputSource(config: XMLCalabashConfig, node: XdmNode): InputSource = {
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

  def configureSerializer(serializer: Serializer, options: Map[QName,String]): Unit = {
    for (opt <- options.keySet) {
      serializer.setOutputProperty(opt, options(opt))
    }
  }

  def serialize(config: XMLCalabashConfig, value: XdmValue, serializer: Serializer): Unit = {
    serialize(config, List(value), serializer)
  }

  def serialize(xproc: XMLCalabashConfig, values: List[XdmValue], serializer: Serializer): Unit = {
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

  private def serializeMap(xproc: XMLCalabashConfig, value: XdmMap, serializer: Serializer): Unit = {
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

  private def serializeArr(xproc: XMLCalabashConfig, arr: XdmArray, serializer: Serializer): Unit = {
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

  def emptyDocument(config: XMLCalabashRuntime): XdmNode = {
    emptyDocument(config, None)
  }

  def emptyDocument(config: XMLCalabashRuntime, baseURI: Option[URI]): XdmNode = {
    val tree = new SaxonTreeBuilder(config)
    tree.startDocument(baseURI)
    tree.endDocument()
    tree.result
  }

  def forceQNameKeys(inputMap: MapItem): XdmMap = {
    var map = new XdmMap()

    val iter = inputMap.keyValuePairs().iterator()
    while (iter.hasNext) {
      val pair = iter.next()
      pair.key.getItemType match {
        case BuiltInAtomicType.STRING =>
          val key = new QName("", "", pair.key.getStringValue)
          map = map.put(new XdmAtomicValue(key), XdmValue.wrap(pair.value))
        case BuiltInAtomicType.QNAME =>
          val qvalue = pair.key.asInstanceOf[QNameValue]
          val key = new QName(qvalue.getPrefix, qvalue.getNamespaceURI, qvalue.getLocalName)
          map = map.put(new XdmAtomicValue(key), XdmValue.wrap(pair.value))
        case _ =>
          // FIXME: not sure this works (given that it doesn't work for QNameValues
          map = map.put(pair.key.asInstanceOf[XdmAtomicValue], XdmValue.wrap(pair.value))
      }
    }

    map
  }

  def urisForPrefixes(node: XdmNode, prefixList: List[String]): Set[String] = {
    val uriList = mutable.HashSet.empty[String]

    val inode = node.getUnderlyingNode
    val inscopeNS = new InscopeNamespaceResolver(inode)
    var all = false

    for (pfx <- prefixList) {
      var found = false

      if (pfx == "#all") {
        found = true
        all = true
      } else {
        if (pfx == "#default") {
          found = (inscopeNS.getURIForPrefix("", true) != null)
          if (found) {
            uriList += inscopeNS.getURIForPrefix("", true)
          }
        } else {
          found = (inscopeNS.getURIForPrefix(pfx, false) != null)
          if (found) {
            uriList += inscopeNS.getURIForPrefix(pfx, false)
          }
        }
      }

      if (!found) {
        throw new RuntimeException("No binding for prefix: " + pfx)
      }
    }

    if (all) {
      val pfxiter = inscopeNS.iteratePrefixes()
      while (pfxiter.hasNext) {
        val pfx = pfxiter.next()
        val isdef = (pfx == "")
        uriList += inscopeNS.getURIForPrefix(pfx, isdef)
      }
    }

    uriList.toSet
  }

  def uniquePrefix(prefixes: Set[String]): String = {
    var acount = 0
    var aprefix = "_0"
    var done = false
    while (!done) {
      acount += 1
      aprefix = s"_$acount"
      done = !prefixes.contains(aprefix)
    }

    aprefix
  }
}
