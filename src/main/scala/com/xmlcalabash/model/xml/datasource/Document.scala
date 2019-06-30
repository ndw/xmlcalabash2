package com.xmlcalabash.model.xml.datasource

import com.jafpl.graph.{Binding, ContainerStart, Graph, Node}
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.model.xml.containers.When
import com.xmlcalabash.model.xml.{Artifact, DeclareStep, IOPort, OptionDecl, Output, PipelineStep, Variable, WithInput, WithOption}
import com.xmlcalabash.runtime.{ExpressionContext, XMLCalabashRuntime, XProcVtExpression, XProcXPathExpression}
import com.xmlcalabash.steps.internal.{EmptyLoader, FileLoader}
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

    if (doc.staticContext.baseURI.isDefined) {
      staticContext.baseURI = doc.staticContext.baseURI.get
    }
    if (doc.staticContext.location.isDefined) {
      staticContext.location = doc.staticContext.location.get
    }

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
    var valid = super.validate()

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
    if (hrefAvt.isDefined) {
      bindingRefs ++= ValueParser.findVariableRefsInAvt(config, hrefAvt.get, location)
    }

    if (_params.isDefined) {
      bindingRefs ++= lexicalVariables(_params.get)
      paramsExpr = Some(_params.get)
    }

    if (_docProps.isDefined) {
      bindingRefs ++= lexicalVariables(_docProps.get)
    }

    valid
  }

  override def makeGraph(graph: Graph, parent: Node) {
    var container = nearestContainer()

    if (container.isInstanceOf[When] && this.parent.get.isInstanceOf[WithInput]) {
      // An inline that serves as the context for a when must be outside of the when!
      container = container.nearestContainer()
    }

    val cnode = container._graphNode.get.asInstanceOf[ContainerStart]

    val context = new ExpressionContext(staticContext)
    val produceDocument = new FileLoader(context, _contentType, _docProps)
    if (location.isDefined) {
      produceDocument.location = location.get
    }
    val docProducer = cnode.addAtomic(produceDocument,"p:document " + name)

    val hrefBinding = cnode.addVariable("href", new XProcVtExpression(context, hrefAvt.get, true))
    graph.addBindingEdge(hrefBinding, docProducer)
    if (paramsExpr.isDefined) {
      val binding = cnode.addVariable("parameters", new XProcXPathExpression(context, paramsExpr.get))
      graph.addBindingEdge(binding, docProducer)
    }

    _graphNode = Some(docProducer)
    config.addNode(docProducer.id, this)

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

        case optDecl: OptionDecl =>
          graph.addBindingEdge(optDecl._graphNode.get.asInstanceOf[Binding], hrefBinding)

        case variable: Variable =>
          graph.addBindingEdge(variable._graphNode.get.asInstanceOf[Binding], hrefBinding)

        case _ =>
          throw new ModelException(ExceptionCode.INTERNAL, s"Unexpected $ref binding: ${bind.get}", location)
      }
    }
  }

  override def makeEdges(graph: Graph, parNode: Node): Unit = {
    var toNode = Option.empty[Node]
    var toPort = ""

    parent.get match {
      case opt: WithOption =>
        toNode = opt._graphNode
        toPort = "source"
      case wi: WithInput =>
        toNode = parent.get.parent.get._graphNode
        toPort = wi.port.get
        if (wi.parent.get.isInstanceOf[When]) {
          toNode = wi.parent.get._graphNode
          toPort = "condition"
        }
      case port: IOPort =>
        toNode = parent.get.parent.get._graphNode
        toPort = port.port.get
      case variable: Variable =>
        toNode = variable._graphNode
        toPort = "source"
      case _ =>
        // It must be an explicit link from somewhere else; make an output to link from
        val out = new Output(config, this, "result", primary=true, sequence=true)
        addChild(out)
    }

    if (toNode.isDefined) {
      graph.addOrderedEdge(_graphNode.get, "result", toNode.get, toPort)
    }

    val docparent = this.parent.get
    val istep = docparent.parent.get
    val container = if (docparent.isInstanceOf[WithInput] && istep.isInstanceOf[When]) {
      istep.nearestContainer()
    } else {
      docparent.nearestContainer()
    }

    var emptyLatch = true
    if (container.isInstanceOf[DeclareStep] && docparent.isInstanceOf[WithInput]) {
      val port = docparent.asInstanceOf[WithInput].port.get
      if (container.input(port).isDefined) {
        if (container.input(port).get.defaultInputs.nonEmpty) {
          graph.addEdge(container._graphNode.get, port, _graphNode.get, "latch")
          emptyLatch = false
        }
      }
    }

    if (emptyLatch) {
      val cnode = container._graphNode.get.asInstanceOf[ContainerStart]
      val context = new ExpressionContext(staticContext)
      val step = new EmptyLoader()
      val emptyReader = cnode.addAtomic(step, "empty")

      config.addNode(emptyReader.id, this)
      graph.addEdge(emptyReader, "result", _graphNode.get, "latch")
    }
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
