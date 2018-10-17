package com.xmlcalabash.model.xml.datasource

import com.jafpl.graph.{Binding, ContainerStart, Graph, Node}
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.model.xml.{Artifact, DeclareStep, IOPort, OptionDecl}
import com.xmlcalabash.runtime.{ExpressionContext, XMLCalabashRuntime, XProcVtExpression, XProcXPathExpression}
import com.xmlcalabash.steps.internal.FileLoader
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Document(override val config: XMLCalabashRuntime,
               override val parent: Option[Artifact])
  extends DataSource(config, parent) {

  private var _href = Option.empty[String]
  private var _params = Option.empty[String]
  private var _docProps = Option.empty[String]
  private var _contentType = Option.empty[MediaType]
  private var hrefAvt = Option.empty[List[String]]
  private var paramsExpr = Option.empty[String]
  private val bindingRefs = mutable.HashSet.empty[QName]

  def this(config: XMLCalabashRuntime, parent: Artifact, doc: Document) = {
    this(config, Some(parent))
    _href = doc._href
    _params = doc._params
    _docProps = doc._docProps
    _contentType = doc._contentType
    _baseURI = doc._baseURI
    hrefAvt = doc.hrefAvt
    paramsExpr = doc.paramsExpr
    bindingRefs.clear()
    bindingRefs ++= doc.bindingRefs
  }

  def this(config: XMLCalabashRuntime, parent: Artifact, href: String) = {
    this(config, Some(parent))
    _href = Some(href)
    hrefAvt = ValueParser.parseAvt(_href.get)
    bindingRefs ++= lexicalVariables(_href.get)
  }

  override def validate(): Boolean = {
    _href = attributes.get(XProcConstants._href)
    _params = attributes.get(XProcConstants._parameters)
    _docProps = attributes.get(XProcConstants._document_properties)
    _contentType = MediaType.parse(attributes.get(XProcConstants._content_type))

    for (key <- List(XProcConstants._href, XProcConstants._document_properties, XProcConstants._parameters,
      XProcConstants._content_type)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (_href.isEmpty) {
      throw new ModelException(ExceptionCode.ATTRREQ, "href", location)
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    if (children.nonEmpty) {
      throw XProcException.xsElementNotAllowed(location, children.head.nodeName)
    }

    hrefAvt = ValueParser.parseAvt(_href.get)
    bindingRefs ++= lexicalVariables(_href.get)

    if (_params.isDefined) {
      bindingRefs ++= lexicalVariables(_params.get)
      paramsExpr = Some(_params.get);
    }

    if (_docProps.isDefined) {
      bindingRefs ++= lexicalVariables(_docProps.get)
    }

    true
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val container = this.parent.get.parent.get.parent.get
    val cnode = container._graphNode.get.asInstanceOf[ContainerStart]
    val context = new ExpressionContext(baseURI, inScopeNS, location)
    val step = new FileLoader(context, _contentType, _docProps)
    val docReader = cnode.addAtomic(step, "document")

    val hrefBinding = cnode.addVariable("href", new XProcVtExpression(context, hrefAvt.get, true))
    graph.addBindingEdge(hrefBinding, docReader)
    if (paramsExpr.isDefined) {
      val binding = cnode.addVariable("parameters", new XProcXPathExpression(context, paramsExpr.get))
      graph.addBindingEdge(binding, docReader)
    }

    _graphNode = Some(docReader)
    config.addNode(docReader.id, this)

    for (ref <- bindingRefs) {
      val bind = findBinding(ref)
      if (bind.isEmpty) {
        throw new ModelException(ExceptionCode.NOBINDING, ref.toString, location)
      }
      bind.get match {
        case declStep: DeclareStep =>
          var optDecl = Option.empty[OptionDecl]
          for (child <- declStep.children) {
            child match {
              case opt: OptionDecl =>
                if (opt.optionName == ref) {
                  optDecl = Some(opt)
                }
              case _ => Unit
            }
          }
          if (optDecl.isEmpty) {
            throw new ModelException(ExceptionCode.NOBINDING, ref.toString, location)
          }
          graph.addBindingEdge(optDecl.get._graphNode.get.asInstanceOf[Binding], hrefBinding)

        case _ =>
          throw new ModelException(ExceptionCode.INTERNAL, s"Unexpected $ref binding: ${bind.get}", location)
      }
    }
  }

  override def makeEdges(graph: Graph, parent: Node): Unit = {
    val toStep = this.parent.get.parent
    val toPort = this.parent.get.asInstanceOf[IOPort].port.get
    graph.addOrderedEdge(_graphNode.get, "result", toStep.get._graphNode.get, toPort)
  }

  override def asXML: xml.Elem = {
    dumpAttr("href", _href)
    dumpAttr("document-properties", _docProps)

    val nodes = ListBuffer.empty[xml.Node]
    nodes += xml.Text("\n")
    for (child <- children) {
      nodes += child.asXML
      nodes += xml.Text("\n")
    }
    new xml.Elem("p", "document", dump_attr.getOrElse(xml.Null),
      namespaceScope, false, nodes:_*)
  }

}
