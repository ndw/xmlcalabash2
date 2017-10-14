package com.xmlcalabash.model.xml

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.model.xml.containers.Container
import com.xmlcalabash.runtime.{ExpressionContext, XProcXPathExpression}
import com.xmlcalabash.util.{SerializationOptions, TypeUtils}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmMap, XdmValue}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Output(override val config: XMLCalabash,
             override val parent: Option[Artifact]) extends IOPort(config, parent) {
  private var serOpts = new SerializationOptions(config)

  def serialization: SerializationOptions = serOpts

  protected[xml] def this(config: XMLCalabash, parent: Artifact, port: String, primary: Boolean, sequence: Boolean) {
    this(config, Some(parent))
    _port = Some(port)
    _primary = Some(primary)
    _sequence = Some(sequence)
  }

  override def validate(): Boolean = {
    var valid = super.validate()

    _port = attributes.get(XProcConstants._port)

    var attr = attributes.get(XProcConstants._primary)
    if (attr.isDefined) {
      attr.get match {
        case "true" => _primary = Some(true)
        case "false" => _primary = Some(false)
        case _ => throw new RuntimeException("primary must be true or false")
      }
    } else {
      _primary = None
    }

    attr = attributes.get(XProcConstants._sequence)
    if (attr.isDefined) {
      attr.get match {
        case "true" => _sequence = Some(true)
        case "false" => _sequence = Some(false)
        case _ => throw new RuntimeException("sequence must be true or false")
      }
    } else {
      _sequence = None
    }

    val ser = attributes.get(XProcConstants._serialization)
    if (ser.isDefined) {
      val opts = mutable.HashMap.empty[QName, XdmAtomicValue]
      val context = new ExpressionContext(baseURI, inScopeNS, location)
      val serAvt = new XProcXPathExpression(context, ser.get)
      val bindingRefs = ValueParser.findVariableRefsInString(config, inScopeNS, ser.get)
      val eval = config.expressionEvaluator
      val message = eval.singletonValue(serAvt, List(), Map(), None)
      message match {
        case item: XPathItemMessage =>
          item.item match {
            case xdmMap: XdmMap =>
              val map = xdmMap.asMap()
              for (key <- map.asScala.keySet) {
                val optkey = TypeUtils.castAsJava(key)
                val optvalue = map.asScala(key)
                val value = optvalue match {
                  case atomic: XdmAtomicValue =>
                    atomic
                  case _ => throw new RuntimeException("Not an atomic value?")
                }
                optkey match {
                  case str: String =>
                    opts.put(new QName("", str), value)
                  case qname: QName =>
                    opts.put(qname, value)
                  case _ => throw new RuntimeException("map key is not a qname")
                }
              }
            case _ => throw new RuntimeException("Not a map?")
          }
        case _ => throw new RuntimeException("Not an item message?")
      }
      serOpts = new SerializationOptions(config, opts.toMap)
    }

    for (key <- List(XProcConstants._port, XProcConstants._sequence, XProcConstants._primary, XProcConstants._serialization)) {
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
          throw new ModelException(ExceptionCode.BADCHILD, child.toString, location)
        }
      }
    } else {
      if (children.nonEmpty) {
        throw new ModelException(ExceptionCode.BADCHILD, children.head.toString, location)
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
