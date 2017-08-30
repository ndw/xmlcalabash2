package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.xmlcalabash.exceptions.ModelException
import com.xmlcalabash.model.util.ParserConfiguration
import net.sf.saxon.s9api.QName

import scala.collection.mutable.ListBuffer

class Serialization(override val config: ParserConfiguration,
                    override val parent: Option[Artifact]) extends Artifact(config, parent) {
  var _port: Option[String] = None
  var _byte_order_mark: Option[Boolean] = None
  var _cdata_section_elements: List[String] = List()
  var _doctype_public: Option[String] = None
  var _doctype_system: Option[String] = None
  var _encoding: Option[String] = None
  var _escape_uri_attributes: Option[Boolean] = None
  var _include_content_type: Option[Boolean] = None
  var _indent: Option[Boolean] = None
  var _media_type: Option[String] = None
  var _method: Option[QName] = None
  var _normalization_form: Option[String] = None
  var _omit_xml_declaration: Option[Boolean] = None
  var _standalone: Option[String] = None
  var _undeclare_prefixes: Option[Boolean] = None
  var _version: Option[String] = None

  override def validate(): Boolean = {
    _port = properties.get(XProcConstants._port)
    _byte_order_mark = lexicalBoolean(properties.get(XProcConstants._byte_order_mark))
    _cdata_section_elements = if (properties.contains(XProcConstants._cdata_section_elements)) {
      properties(XProcConstants._cdata_section_elements).split("\\s+").toList
    } else {
      List()
    }
    _doctype_public = properties.get(XProcConstants._doctype_public)
    _doctype_system = properties.get(XProcConstants._doctype_system)
    _encoding = properties.get(XProcConstants._encoding)
    _escape_uri_attributes = lexicalBoolean(properties.get(XProcConstants._escape_uri_attributes))
    _include_content_type = lexicalBoolean(properties.get(XProcConstants._include_content_type))
    _indent = lexicalBoolean(properties.get(XProcConstants._indent))
    _media_type = properties.get(XProcConstants._media_type)
    _method = lexicalQName(properties.get(XProcConstants._method))
    _normalization_form = properties.get(XProcConstants._normalization_form)
    _omit_xml_declaration = lexicalBoolean(properties.get(XProcConstants._omit_xml_declaration))
    _standalone = properties.get(XProcConstants._standalone)
    _undeclare_prefixes = lexicalBoolean(properties.get(XProcConstants._undeclare_prefixes))
    _version = properties.get(XProcConstants._version)

    for (key <- List(XProcConstants._port, XProcConstants._byte_order_mark,
      XProcConstants._cdata_section_elements, XProcConstants._doctype_public,
      XProcConstants._doctype_system, XProcConstants._encoding,
      XProcConstants._escape_uri_attributes, XProcConstants._include_content_type,
      XProcConstants._indent, XProcConstants._media_type, XProcConstants._method,
      XProcConstants._normalization_form, XProcConstants._omit_xml_declaration,
      XProcConstants._standalone, XProcConstants._undeclare_prefixes, XProcConstants._version)) {
      if (properties.contains(key)) {
        properties.remove(key)
      }
    }

    if (properties.nonEmpty) {
      val key = properties.keySet.head
      throw new ModelException("badopt", s"Unexpected attribute: ${key.getLocalName}")
    }

    if (_port.isEmpty) {
      throw new ModelException("portreq", "Port is required")
    }

    if (parent.isDefined && !parent.get.outputPorts.contains(_port.get)) {
      throw new ModelException("badport", s"No such port: ${_port.get}")
    }

    if (_standalone.isDefined) {
      if (!List("true", "false", "omit").contains(_standalone.get)) {
        throw new ModelException("badstandalone",
          s"Invalid standalone value: ${_standalone.get}")
      }
      if (children.nonEmpty) {
        throw new ModelException("badelem", s"Unexpected element: ${children.head}")
      }
    }

    valid
  }

  override def asXML: xml.Elem = {
    dumpAttr("cdata-section-elements", if (_cdata_section_elements.nonEmpty) {
      var s = ""
      for (elem <- _cdata_section_elements) {
        if (s != "") {
          s += " "
        }
        s += elem
      }
      Some(s)
    } else {
      None
    })
    dumpAttr("byte-order-mark", _byte_order_mark)
    dumpAttr("doctype-public", _doctype_public)
    dumpAttr("doctype-system", _doctype_system)
    dumpAttr("encoding", _encoding)
    dumpAttr("escape-uri-attributes", _escape_uri_attributes)
    dumpAttr("include-content-type", _include_content_type)
    dumpAttr("indent", _indent)
    dumpAttr("media-type", _media_type)
    dumpAttr("normalization-form", _normalization_form)
    dumpAttr("omit-xml-declaration", _omit_xml_declaration)
    dumpAttr("standalone", _standalone)
    dumpAttr("undeclare-prefixes", _undeclare_prefixes)
    dumpAttr("version", _version)
    dumpAttr("method", _method)
    dumpAttr("port", _port)

    val nodes = ListBuffer.empty[xml.Node]
    if (children.nonEmpty) {
      nodes += xml.Text("\n")
    }
    for (child <- children) {
      nodes += child.asXML
      nodes += xml.Text("\n")
    }
    new xml.Elem("p", "serialization", dump_attr.get, namespaceScope, false, nodes: _*)
  }

  override def makeGraph(graph: Graph, parent: Node) {
    // no direct contribution
  }

  override def makeEdges(graph: Graph, parent: Node) {
    // no direct contribution
  }
}
