package com.xmlcalabash.model.xml

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.containers.Container
import com.xmlcalabash.runtime.{ExpressionContext, XMLCalabashRuntime, XProcXPathExpression}
import com.xmlcalabash.util.{MediaType, TypeUtils}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmMap}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Output(override val config: XMLCalabashRuntime,
             override val parent: Option[Artifact]) extends IOPort(config, parent) {
  private var serOpts = Map.empty[QName,String]
  protected var _contentTypes = ListBuffer.empty[MediaType]

  def serialization: Map[QName,String] = serOpts
  def contentTypes: List[MediaType] = _contentTypes.toList

  protected[xml] def this(config: XMLCalabashRuntime, parent: Artifact, port: String, primary: Boolean, sequence: Boolean) {
    this(config, Some(parent))
    _port = Some(port)
    _primary = Some(primary)
    _sequence = Some(sequence)
  }

  override def validate(): Boolean = {
    var valid = super.validate()

    _port = attributes.get(XProcConstants._port)
    _primary = lexicalBoolean(attributes.get(XProcConstants._primary))
    _sequence = lexicalBoolean(attributes.get(XProcConstants._sequence))

    val ctypes = attributes.get(XProcConstants._content_types)
    if (ctypes.isDefined) {
      try {
        _contentTypes ++= MediaType.parseList(ctypes.get)
      } catch {
        case ex: XProcException =>
          if (ex.code == XProcException.xc0070) {
            // Map to the static error...
            throw XProcException.xsUnrecognizedContentType(ex.details.head.toString, ex.location)
          } else {
            throw ex
          }
        case ex: Exception => throw ex
      }
    } else {
      _contentTypes += MediaType.OCTET_STREAM
    }

    val ser = attributes.get(XProcConstants._serialization)
    if (ser.isDefined) {
      val opts = mutable.HashMap.empty[QName, String]
      val context = new ExpressionContext(staticContext)
      val serAvt = new XProcXPathExpression(context, ser.get)
      val bindingRefs = lexicalVariables(ser.get)
      val staticVariableMap = mutable.HashMap.empty[String, XdmValueItemMessage]
      /*
      for (vref <- bindingRefs) {
        val msg = staticValue(vref)
        if (msg.isDefined) {
          staticVariableMap.put(vref.getClarkName, msg.get)
        } else {
          throw new ModelException(ExceptionCode.NOBINDING, vref.toString, location)
        }
      }
     */
      val eval = config.expressionEvaluator
      val message = eval.singletonValue(serAvt, List(), staticVariableMap.toMap, None)
      message match {
        case item: XdmValueItemMessage =>
          item.item match {
            case xdmMap: XdmMap =>
              val map = xdmMap.asMap()
              for (key <- map.asScala.keySet) {
                val optkey = TypeUtils.castAsJava(key)
                val optvalue = map.asScala(key)
                val value = optvalue match {
                  case atomic: XdmAtomicValue =>
                    atomic
                  case _ => throw XProcException.dynamicError(47, optvalue, location)
                }
                optkey match {
                  case str: String =>
                    opts.put(new QName("", str), value.getStringValue)
                  case qname: QName =>
                    opts.put(qname, value.getStringValue)
                  case _ => throw XProcException.dynamicError(46, optkey, location)
                }
              }
            case _ => throw XProcException.dynamicError(48, item.item, location)
          }
        case _ => throw XProcException.xiBadMessage(message, location)
      }
      serOpts = opts.toMap
    }

    for (key <- List(XProcConstants._port, XProcConstants._sequence, XProcConstants._primary,
      XProcConstants._serialization, XProcConstants._content_types)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    if (parent.isDefined && parent.get.isInstanceOf[Container]) {
      for (child <- children) {
        if (dataSourceClasses.contains(child.getClass)) {
          valid = valid && child.validate()
        } else {
          throw XProcException.xsElementNotAllowed(location, child.nodeName)
        }
      }
    } else {
      if (children.nonEmpty) {
        throw XProcException.xsElementNotAllowed(location, children.head.nodeName)
      }
    }

    valid
  }

  override def makeGraph(graph: Graph, parent: Node) {
    // Process the children in the context of our parent
    for (child <- children) {
      child.makeGraph(graph, parent)
    }
  }

  override def asXML: xml.Elem = {
    dumpAttr("port", _port)
    dumpAttr("sequence", _sequence)
    dumpAttr("primary", _primary)
    dumpAttr("id", id.toString)

    val nodes = ListBuffer.empty[xml.Node]
    if (children.nonEmpty) {
      nodes += xml.Text("\n")
    }
    for (child <- children) {
      nodes += child.asXML
      nodes += xml.Text("\n")
    }
    new xml.Elem("p", "output", dump_attr.get, namespaceScope, false, nodes:_*)
  }

}
