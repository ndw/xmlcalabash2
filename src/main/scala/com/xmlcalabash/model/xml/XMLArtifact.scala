package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.util.{RelevantNodes, TreeWriter}
import net.sf.saxon.s9api.{Axis, QName, XdmNode}

import scala.collection.immutable.{HashMap, Set}

/**
  * Created by ndw on 10/4/16.
  */
class XMLArtifact {
  protected val properties = HashMap(
    XProcConstants.p_declare_step -> Set(XProcConstants._name, XProcConstants._type, XProcConstants._psvi_required,
      XProcConstants._xpath_version, XProcConstants._version, XProcConstants._exclude_inline_prefixes),
    XProcConstants.p_pipeline -> Set(XProcConstants._name, XProcConstants._type, XProcConstants._psvi_required,
      XProcConstants._xpath_version, XProcConstants._version, XProcConstants._exclude_inline_prefixes),
    XProcConstants.p_input -> Set(XProcConstants._port, XProcConstants._sequence, XProcConstants._primary,
      XProcConstants._kind, XProcConstants._select),
    XProcConstants.p_namespaces -> Set(XProcConstants._binding, XProcConstants._element),
    XProcConstants.p_for_each -> Set(XProcConstants._name),
    XProcConstants.p_viewport -> Set(XProcConstants._name, XProcConstants._match),
    XProcConstants.p_choose -> Set(XProcConstants._name),
    XProcConstants.p_when -> Set(XProcConstants._test),
    XProcConstants.p_otherwise -> Set.empty[QName],
    XProcConstants.p_group -> Set(XProcConstants._name),
    XProcConstants.p_try -> Set(XProcConstants._name),
    XProcConstants.p_catch -> Set(XProcConstants._name),
    XProcConstants.p_iteration_source -> Set(XProcConstants._select),
    XProcConstants.p_viewport_source -> Set.empty[QName],
    XProcConstants.p_output -> Set(XProcConstants._port, XProcConstants._sequence, XProcConstants._primary),
    XProcConstants.p_log -> Set(XProcConstants._port, XProcConstants._href),
    XProcConstants.p_variable -> Set(XProcConstants._name, XProcConstants._select),
    XProcConstants.p_option -> Set(XProcConstants._name, XProcConstants._required, XProcConstants._select),
    XProcConstants.p_with_option -> Set(XProcConstants._name, XProcConstants._select),
    XProcConstants.p_with_param -> Set(XProcConstants._name, XProcConstants._select, XProcConstants._port),
    XProcConstants.p_library -> Set(XProcConstants._psvi_required, XProcConstants._xpath_version,
      XProcConstants._exclude_inline_prefixes, XProcConstants._version),
    XProcConstants.p_import -> Set(XProcConstants._href),
    XProcConstants.p_serialization -> Set(XProcConstants._port, XProcConstants._byte_order_mark,
      XProcConstants._cdata_section_elements,
      XProcConstants._doctype_public, XProcConstants._doctype_system, XProcConstants._encoding,
      XProcConstants._escape_uri_attributes, XProcConstants._include_content_type,
      XProcConstants._indent, XProcConstants._media_type, XProcConstants._method,
      XProcConstants._normalization_form, XProcConstants._omit_xml_declaration,
      XProcConstants._standalone, XProcConstants._undeclare_prefixes, XProcConstants._version),
    XProcConstants.p_xpath_context -> Set.empty[QName],
    XProcConstants.p_empty -> Set.empty[QName],
    XProcConstants.p_document -> Set(XProcConstants._href),
    XProcConstants.p_inline -> Set(XProcConstants._exclude_inline_prefixes),
    XProcConstants.p_data -> Set(XProcConstants._href, XProcConstants._wrapper, XProcConstants._wrapper_prefix,
      XProcConstants._wrapper_namespaces, XProcConstants._content_type),
    XProcConstants.p_pipe -> Set(XProcConstants._step, XProcConstants._port)
  )

  protected var _xmlname = "XMLArtifact"
  protected var _parent: Option[XMLArtifact] = _
  protected var _node: XdmNode = _
  protected val _nsbindings = collection.mutable.Set.empty[Namespace]
  protected val prop = collection.mutable.Set.empty[Attribute]
  protected val attr = collection.mutable.Set.empty[Attribute]
  protected val children = collection.mutable.ListBuffer.empty[XMLArtifact]

  def initNode(node: XdmNode, parent: Option[XMLArtifact]) {
    _xmlname = node.getNodeName.getLocalName
    _node = node
    _parent = parent
  }

  def xmlname = _xmlname
  def xmlname_=(name: String): Unit = {
    _xmlname = name
  }

  def addChild(child: XMLArtifact): Unit = {
    children += child
  }

  def parse(node: XdmNode): Unit = {
    parseNamespaces(node)
    parseAttributes(node)
    parseChildren(node)
  }

  def parseSubpipeline(node: XdmNode): Unit = {
    parseNamespaces(node)
    parseAttributes(node)
    parseChildren(node, stepsAllowed = true)
  }

  private def parseNamespaces(node: XdmNode): Unit = {
    for (childitem <- RelevantNodes.filter(node, Axis.NAMESPACE)) {
      val child = childitem.asInstanceOf[XdmNode]
      val prefix = if (child.getNodeName == null) {
        ""
      } else {
        child.getNodeName.getLocalName
      }

      // This is kind of inefficient, but so be it
      val ns = new Namespace(prefix, child.getStringValue)
      var found = false
      var parent = _parent
      while (!found && parent.isDefined) {
        found = parent.get._nsbindings.contains(ns)
        parent = parent.get._parent
      }

      if (!found) {
        _nsbindings.add(ns)
      }
    }
  }

  private def parseAttributes(node: XdmNode): Unit = {
    var propnames = properties.getOrElse(node.getNodeName, Set.empty[QName])
    for (childitem <- RelevantNodes.filter(node, Axis.ATTRIBUTE)) {
      val child = childitem.asInstanceOf[XdmNode]
      if (propnames.contains(child.getNodeName)) {
        prop += new Attribute(child)
      } else {
        attr += new Attribute(child)
      }
    }
  }

  private def parseChildren(node: XdmNode): Unit = {
    parseChildren(node, false)
  }

  def parseChildren(node: XdmNode, stepsAllowed: Boolean): Unit = {
    for (childitem <- RelevantNodes.filter(node, Axis.CHILD)) {
      val child = childitem.asInstanceOf[XdmNode]
      child.getNodeName match {
        case XProcConstants.p_catch => children += new Catch(child, Some(this))
        case XProcConstants.p_choose => children += new Choose(child, Some(this))
        case XProcConstants.p_data => children += new Data(child, Some(this))
        case XProcConstants.p_declare_step => children += new DeclareStep(child, Some(this))
        case XProcConstants.p_document => children += new Document(child, Some(this))
        case XProcConstants.p_empty => children += new Empty(child, Some(this))
        case XProcConstants.p_for_each => children += new ForEach(child, Some(this))
        case XProcConstants.p_group => children += new Group(child, Some(this))
        case XProcConstants.p_import => children += new Import(child, Some(this))
        case XProcConstants.p_inline => children += new Inline(child, Some(this))
        case XProcConstants.p_input => children += new Input(child, Some(this))
        case XProcConstants.p_iteration_source => children += new IterationSource(child, Some(this))
        case XProcConstants.p_library => children += new Library(child, Some(this))
        case XProcConstants.p_log => children += new Log(child, Some(this))
        case XProcConstants.p_namespaces => children += new Namespaces(child, Some(this))
        case XProcConstants.p_option => children += new DeclOption(child, Some(this))
        case XProcConstants.p_otherwise => children += new Otherwise(child, Some(this))
        case XProcConstants.p_output => children += new Output(child, Some(this))
        case XProcConstants.p_pipe => children += new Pipe(child, Some(this))
        case XProcConstants.p_pipeline => children += new Pipeline(child, Some(this))
        case XProcConstants.p_serialization => children += new Serialization(child, Some(this))
        case XProcConstants.p_try => children += new Try(child, Some(this))
        case XProcConstants.p_variable => children += new Variable(child, Some(this))
        case XProcConstants.p_viewport => children += new Viewport(child, Some(this))
        case XProcConstants.p_viewport_source => children += new ViewportSource(child, Some(this))
        case XProcConstants.p_when => children += new When(child, Some(this))
        case XProcConstants.p_with_option => children += new WithOption(child, Some(this))
        case XProcConstants.p_with_param => children += new WithParam(child, Some(this))
        case XProcConstants.p_xpath_context => children += new XPathContext(child, Some(this))
        case XProcConstants.p_pipeinfo => Unit
        case XProcConstants.p_documentation => Unit
        case _ =>
          if (stepsAllowed) {
            children += new AtomicStep(child, Some(this))
          } else {
            this match {
              case a: Inline => Unit
              case a: Input => Unit
              case a: Output => Unit
              case _ => println("Unexpected: " + child.getNodeName)
            }
            children += new XMLLiteral(child, Some(this))
          }
      }
    }
  }

  def fixup(): Unit = {
    for (child <- children) {
      child.fixup()
    }
  }

  def dump(tree: TreeWriter): Unit = {
    tree.addStartElement(XProcConstants.px(_xmlname))
    tree.addAttribute(XProcConstants.px("obj"), this.toString)

    for (ns <- _nsbindings) {
      tree.addNamespace(ns.prefix, ns.uri)
    }

    for (att <- prop) {
      tree.addAttribute(att.name, att.value)
    }
    for (att <- attr) {
      tree.addAttribute(att.name, att.value)
    }
    for (child <- children) {
      child.dump(tree)
    }
    tree.addEndElement()
  }
}
