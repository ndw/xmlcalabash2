package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.decl.StepLibrary
import com.xmlcalabash.model.xml.util.{RelevantNodes, TreeWriter}
import com.xmlcalabash.util.UniqueId
import net.sf.saxon.s9api.{Axis, QName, XdmNode}

import scala.collection.immutable.{HashMap, Set}
import scala.collection.mutable

/**
  * Created by ndw on 10/4/16.
  */
class XMLArtifact(val node: Option[XdmNode], val parent: Option[XMLArtifact]) {
  protected val properties = HashMap(
    XProcConstants.p_declare_step -> Set(XProcConstants._name, XProcConstants._type, XProcConstants._psvi_required,
      XProcConstants._xpath_version, XProcConstants._version, XProcConstants._exclude_inline_prefixes),
    XProcConstants.p_pipeline -> Set(XProcConstants._name, XProcConstants._type, XProcConstants._psvi_required,
      XProcConstants._xpath_version, XProcConstants._version, XProcConstants._exclude_inline_prefixes),
    XProcConstants.p_input -> Set(XProcConstants._port, XProcConstants._sequence, XProcConstants._primary,
      XProcConstants._kind, XProcConstants._select, XProcConstants._step),
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
    XProcConstants.p_output -> Set(XProcConstants._port, XProcConstants._sequence, XProcConstants._primary,
      XProcConstants._step),
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

  val uid = UniqueId.nextId
  private[xml] var _drp: Option[InputOrOutput] = None
  protected var _xmlname = "XMLArtifact"
  protected val _nsbindings = collection.mutable.Set.empty[Namespace]
  protected val _prop = collection.mutable.Set.empty[Attribute]
  protected val _attr = collection.mutable.Set.empty[Attribute]
  protected val _children = collection.mutable.ListBuffer.empty[XMLArtifact]
  protected var _synthetic = true

  if (node.isDefined) {
    _xmlname = node.get.getNodeName.getLocalName
    _synthetic = false
    parse(node)
  }

  def xmlname = _xmlname
  def xmlname_=(name: String): Unit = {
    _xmlname = name
  }

  def children = _children
  def synthetic = _synthetic
  def defaultReadablePort = _drp

  def addChild(child: XMLArtifact): Unit = {
    _children += child
  }

  def parse(node: Option[XdmNode]): Unit = {
    if (node.isDefined) {
      parseNamespaces(node.get)
      parseAttributes(node.get)
      parseChildren(node.get)
    }
  }

  private[model] def parseNamespaces(node: XdmNode): Unit = {
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
      var p = parent
      while (!found && p.isDefined) {
        found = p.get._nsbindings.contains(ns)
        p = p.get.parent
      }

      if (!found) {
        _nsbindings.add(ns)
      }
    }
  }

  private[model] def parseAttributes(node: XdmNode): Unit = {
    val propnames = properties.getOrElse(node.getNodeName, Set.empty[QName])
    for (childitem <- RelevantNodes.filter(node, Axis.ATTRIBUTE)) {
      val child = childitem.asInstanceOf[XdmNode]
      if (propnames.contains(child.getNodeName)) {
        _prop += new Attribute(child)
      } else {
        _attr += new Attribute(child)
      }
    }
  }

  private def parseChildren(node: XdmNode): Unit = {
    parseChildren(node, false)
  }

  private[model] def parseChildren(node: XdmNode, stepsAllowed: Boolean): Unit = {
    for (childitem <- RelevantNodes.filter(node, Axis.CHILD)) {
      val child = childitem.asInstanceOf[XdmNode]
      child.getNodeName match {
        case XProcConstants.p_catch => _children += new Catch(Some(child), Some(this))
        case XProcConstants.p_choose => _children += new Choose(Some(child), Some(this))
        case XProcConstants.p_data => _children += new Data(Some(child), Some(this))
        case XProcConstants.p_declare_step => _children += new DeclareStep(Some(child), Some(this))
        case XProcConstants.p_document => _children += new Document(Some(child), Some(this))
        case XProcConstants.p_empty => _children += new Empty(Some(child), Some(this))
        case XProcConstants.p_for_each => _children += new ForEach(Some(child), Some(this))
        case XProcConstants.p_group => _children += new Group(Some(child), Some(this))
        case XProcConstants.p_import => _children += new Import(Some(child), Some(this))
        case XProcConstants.p_inline => _children += new Inline(Some(child), Some(this))
        case XProcConstants.p_input => _children += new Input(Some(child), Some(this))
        case XProcConstants.p_iteration_source => _children += new IterationSource(Some(child), Some(this))
        case XProcConstants.p_library => _children += new Library(Some(child), Some(this))
        case XProcConstants.p_log => _children += new Log(Some(child), Some(this))
        case XProcConstants.p_namespaces => _children += new Namespaces(Some(child), Some(this))
        case XProcConstants.p_option => _children += new DeclOption(Some(child), Some(this))
        case XProcConstants.p_otherwise => _children += new Otherwise(Some(child), Some(this))
        case XProcConstants.p_output => _children += new Output(Some(child), Some(this))
        case XProcConstants.p_pipe => _children += new Pipe(Some(child), Some(this))
        case XProcConstants.p_pipeline => _children += new Pipeline(Some(child), Some(this))
        case XProcConstants.p_serialization => _children += new Serialization(Some(child), Some(this))
        case XProcConstants.p_try => _children += new Try(Some(child), Some(this))
        case XProcConstants.p_variable => _children += new Variable(Some(child), Some(this))
        case XProcConstants.p_viewport => _children += new Viewport(Some(child), Some(this))
        case XProcConstants.p_viewport_source => _children += new ViewportSource(Some(child), Some(this))
        case XProcConstants.p_when => _children += new When(Some(child), Some(this))
        case XProcConstants.p_with_option => _children += new WithOption(Some(child), Some(this))
        case XProcConstants.p_with_param => _children += new WithParam(Some(child), Some(this))
        case XProcConstants.p_xpath_context => _children += new XPathContext(Some(child), Some(this))
        case XProcConstants.p_pipeinfo => Unit
        case XProcConstants.p_documentation => Unit
        case _ =>
          if (stepsAllowed) {
            _children += new AtomicStep(Some(child), Some(this))
          } else {
            this match {
              case a: Inline => Unit
              case a: Input => Unit
              case a: Output => Unit
              case _ => println("Unexpected: " + child.getNodeName)
            }
            _children += new XMLLiteral(Some(child), Some(this))
          }
      }
    }
  }

  def property(name: QName): Option[Attribute] = {
    for (p <- _prop) {
      if (p.name == name) {
        return Some(p)
      }
    }
    None
  }

  def addProperty(name: QName, value: String): Unit = {
    val p = property(name)
    if (p.isEmpty) {
      _prop.add(new Attribute(name, value))
    }
  }

  def setProperty(name: QName, value: String): Unit = {
    val p = property(name)
    if (p.isDefined) {
      _prop -= p.get
    }
    _prop.add(new Attribute(name, value))
  }

  def removeProperty(name: QName): Unit = {
    val p = property(name)
    if (p.isDefined) {
      _prop -= p.get
    }
  }

  def findInScopeStep(name: String): Option[Step] = {
    if (parent.isDefined) {
      parent.get.findInScopeStep(name)
    } else {
      None
    }
  }

  def bindings(): List[Binding] = {
    val bind = mutable.ListBuffer.empty[Binding]
    for (child <- _children) {
      child match {
        case b: Binding => bind += b
        case _ => Unit
      }
    }
    bind.toList
  }

  // ==================================================================================

  def fixup(): Unit = {
    // Fixup is driven from the top-level artifact
  }

  def findDeclarations(decls: List[StepLibrary]): Unit = {
    for (child <- _children) { child.findDeclarations(decls) }
  }

  def makeInputsOutputsExplicit(): Unit = {
    for (child <- _children) { child.makeInputsOutputsExplicit() }
  }

  def addDefaultReadablePort(port: Option[InputOrOutput]): Unit = {
    _drp = port
    for (child <- _children) { child.addDefaultReadablePort(port) }
  }

  def fixUnwrappedInlines(): Unit = {
    for (child <- _children) { child.fixUnwrappedInlines() }
  }

  def fixBindingsOnIO(): Unit = {
    for (child <- _children) { child.fixBindingsOnIO() }
  }

  def findPipeBindings(): Unit = {
    for (child <- _children) { child.findPipeBindings() }
  }

  // ==================================================================================

  override def toString: String = {
    "[" + _xmlname + ":" + uid + "]"
  }

  // ==================================================================================

  def dump(tree: TreeWriter): Unit = {
    tree.addStartElement(XProcConstants.px(_xmlname))
    tree.addAttribute(XProcConstants.px("id"), this.toString)
    if (synthetic) {
      tree.addAttribute(XProcConstants.px("synthetic"), "true")
    }

    dumpAdditionalAttributes(tree)

    for (ns <- _nsbindings) {
      tree.addNamespace(ns.prefix, ns.uri)
    }

    for (att <- _prop) {
      tree.addAttribute(att.name, att.value)
    }
    for (att <- _attr) {
      tree.addAttribute(att.name, att.value)
    }
    for (child <- _children) {
      child.dump(tree)
    }
    tree.addEndElement()
  }

  def dumpAdditionalAttributes(tree: TreeWriter): Unit = {
    // nop
  }
}
