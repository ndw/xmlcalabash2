package com.xmlcalabash.model.xml

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.core.{XProcConstants, XProcEngine}
import com.xmlcalabash.model.xml.bindings._
import com.xmlcalabash.model.xml.decl.StepLibrary
import com.xmlcalabash.model.xml.util.{RelevantNodes, TreeWriter}
import com.xmlcalabash.util.UniqueId
import net.sf.saxon.s9api.{Axis, QName, XdmNode}
import org.slf4j.LoggerFactory

import scala.collection.immutable.{HashMap, Set}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/4/16.
  */
abstract class Artifact(val node: Option[XdmNode], val parent: Option[Artifact]) {
  protected val stepProperties = HashMap(
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

  protected var _xmlname = "XMLArtifact"
  private[xml] var _drp: Option[InputOrOutput] = None
  protected val _nsbindings = collection.mutable.Set.empty[Namespace]
  protected val _prop = collection.mutable.Set.empty[Attribute]
  protected val _attr = collection.mutable.Set.empty[Attribute]
  protected val _children = collection.mutable.ListBuffer.empty[Artifact]
  protected var _synthetic = true
  protected var _valid = true // Innocent until proven guilty
  protected val logger =  LoggerFactory.getLogger(this.getClass)

  val uid = UniqueId.nextId

  if (node.isDefined) {
    _xmlname = node.get.getNodeName.getLocalName
    _synthetic = false
    //parse(node)
  }

  def xmlname = _xmlname
  def xmlname_=(name: String): Unit = {
    _xmlname = name
  }

  def nsbindings: Map[String, String] = {
    val map = mutable.HashMap.empty[String, String]
    for (ns <- _nsbindings) {
      map.put(ns.prefix, ns.uri)
    }
    map.toMap
  }

  def root: PipelineDocument = {
    var p = this
    while (p.parent.isDefined) {
      p = p.parent.get
    }
    p.asInstanceOf[PipelineDocument]
  }

  def children = _children
  def synthetic = _synthetic
  def defaultReadablePort = _drp

  def addChild(child: Artifact): Unit = {
    _children += child
  }

  def insertBefore(node: Artifact, newChild: Artifact): Unit = {
    val newChildren = ListBuffer.empty[Artifact]
    for (child <- _children) {
      if (child == node) {
        newChildren += newChild
      }
      newChildren += child
    }
    _children.clear()
    _children ++= newChildren
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
    val propnames = stepProperties.getOrElse(node.getNodeName, Set.empty[QName])
    for (childitem <- RelevantNodes.filter(node, Axis.ATTRIBUTE)) {
      val child = childitem.asInstanceOf[XdmNode]
      if (propnames.contains(child.getNodeName)) {
        _prop += new Attribute(child)
      } else {
        _attr += new Attribute(child)
      }
    }
  }

  def properties(): Set[QName] = {
    val names = mutable.Set.empty[QName]
    for (p <- _prop) {
      names += p.name
    }
    names.toSet
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

  def attributes(): Set[QName] = {
    _attr.collect { case a: Attribute => a.name }.toSet
  }

  def attribute(name: QName): Option[Attribute] = {
    var attr: Option[Attribute] = None
    for (a <- _attr) {
      if (a.name == name) {
        attr = Some(a)
      }
    }
    attr
  }

  def removeAttribute(name: QName): Unit = {
    val a = attribute(name)
    if (a.isDefined) {
      _attr -= a.get
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
    // Name bindings don't count...
    val bind = mutable.ListBuffer.empty[Binding]
    for (child <- _children) {
      child match {
        case n: NamePipe => Unit
        case b: Binding => bind += b
        case _ => Unit
      }
    }
    bind.toList
  }

  private[xml] def adjustPortReference(fromPort: InputOrOutput, toPort: InputOrOutput): Unit = {
    for (child <- _children) { child.adjustPortReference(fromPort, toPort) }
  }

  // ==================================================================================

  private[xml] def valid(listener: XMLErrorListener): Boolean = {
    var valid = true
    for (child <- children) {
      valid = valid && child.valid(listener)
    }
    valid
  }


  def findDeclarations(decls: List[StepLibrary]): Unit = {
    for (child <- _children) { child.findDeclarations(decls) }
  }

  def findNameDecl(varname: QName, ref: Artifact): Option[NameDecl] = {
    if (parent.isDefined) {
      parent.get.findNameDecl(varname, this)
    } else {
      None
    }
  }

  def makeInputsOutputsExplicit(): Unit = {
    for (child <- _children) { child.makeInputsOutputsExplicit() }
  }

  def promoteShortcutOptions(): Unit = {
    for (child <- _children) { child.promoteShortcutOptions() }
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

  def hoistOptions(): Unit = {
    for (child <- _children) { child.hoistOptions() }
  }

  // ==================================================================================

  def buildGraph(graph: Graph, engine: XProcEngine): Unit = {
    // nop
  }

  private[xml] def buildNodes(graph: Graph, engine: XProcEngine, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    for (child <- children) { child.buildNodes(graph, engine, nodeMap) }
  }

  private[xml] def buildEdges(graph: Graph, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    for (child <- children) { child.buildEdges(graph, nodeMap) }
  }

  // ==================================================================================

  override def toString: String = {
    _xmlname + "_" + uid
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
