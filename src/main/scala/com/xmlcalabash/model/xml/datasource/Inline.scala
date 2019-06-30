package com.xmlcalabash.model.xml.datasource

import com.jafpl.graph.{Binding, ContainerStart, Graph, Node}
import com.jafpl.messages.Message
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.model.xml.containers.When
import com.xmlcalabash.model.xml.{Artifact, DeclareStep, IOPort, OptionDecl, Output, Variable, WithInput, WithOption}
import com.xmlcalabash.runtime.{ExpressionContext, XMLCalabashRuntime}
import com.xmlcalabash.steps.internal.{EmptyLoader, InlineLoader}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Inline(override val config: XMLCalabashRuntime,
             override val parent: Option[Artifact],
             val isImplicit: Boolean,
             val excludeUriBindings: Set[String],
             val nodes: List[XdmNode]) extends DataSource(config, parent) {
  private var _documentProperties = Option.empty[String]
  private var _encoding = Option.empty[String]
  private var _contentType = Option.empty[MediaType]
  protected[xml] val variableRefs = mutable.HashSet.empty[QName]

  def this(config: XMLCalabashRuntime, parent: Option[Artifact], excludeUriBindings: Set[String], nodes: List[XdmNode]) {
    this(config, parent, false, excludeUriBindings, nodes)
  }

  def this(config: XMLCalabashRuntime, parent: Artifact, inline: Inline) {
    this(config, Some(parent), inline.isImplicit, inline.excludeUriBindings, inline.nodes)
    expandText = inline.expandText
    _documentProperties = inline._documentProperties
    _encoding = inline._encoding
    _contentType = inline._contentType
    variableRefs.clear()
    variableRefs ++= inline.variableRefs
  }

  override protected[xml] def parse(node: XdmNode): Unit = {
    super.parse(node)
    if (node.getNodeName != XProcConstants.p_inline) {
      // If this isn't a literal inline, then the attributes aren't for the inline
      attributes.clear()
    }
  }

  override def validate(): Boolean = {
    var valid = super.validate()

    _documentProperties = attributes.get(XProcConstants._document_properties)
    _encoding = attributes.get(XProcConstants._encoding)
    _contentType = MediaType.parse(attributes.get(XProcConstants._content_type))

    for (key <- List(XProcConstants._exclude_inline_prefixes, XProcConstants._expand_text,
      XProcConstants._document_properties, XProcConstants._encoding, XProcConstants._content_type)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      throw new ModelException(ExceptionCode.BADATTR, attributes.keySet.head.toString, location)
    }

    checkForBindings()

    valid
  }

  protected[xml] def checkForBindings(): Unit = {
    for (node <- nodes) {
      variableRefs ++= ValueParser.findVariableRefs(config, node, expandText, location)
      //findVariableRefs(node, _expandText)
      if (_documentProperties.isDefined) {
        variableRefs ++= ValueParser.findVariableRefsInString(config, _documentProperties.get, location)
        //findVariableRefsInString(_documentProperties.get)
      }
    }
  }

  override def makeGraph(graph: Graph, parNode: Node) {
    var container = nearestContainer()

    if (parent.isDefined && parent.get.parent.isDefined) {
      if (parent.get.isInstanceOf[WithInput] && parent.get.parent.get.isInstanceOf[When]) {
        // An inline that serves as the context for a when must be outside of the when!
        container = container.nearestContainer()
      }
    }

    val cnode = container._graphNode.get.asInstanceOf[ContainerStart]

    val context = new ExpressionContext(staticContext)
    val produceInline = new InlineLoader(baseURI, nodes, context, expandText, excludeUriBindings, _contentType, _documentProperties, _encoding)
    if (location.isDefined) {
      produceInline.location = location.get
    }
    val inlineProducer = cnode.addAtomic(produceInline, "p:inline " + name)

    _graphNode = Some(inlineProducer)
    config.addNode(inlineProducer.id, this)

    for (ref <- variableRefs) {
      val bind = findBinding(ref)
      if (bind.isDefined) {
        bind.get match {
          case declStep: DeclareStep =>
            // ???
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
            graph.addBindingEdge(optDecl.get._graphNode.get.asInstanceOf[Binding], inlineProducer)
          case optDecl: OptionDecl =>
            if (!optDecl.static) {
              graph.addBindingEdge(optDecl._graphNode.get.asInstanceOf[Binding], inlineProducer)
            }
          case varDecl: Variable =>
            if (!varDecl.static) {
              graph.addBindingEdge(varDecl._graphNode.get.asInstanceOf[Binding], inlineProducer)
            }
          case _ =>
            throw new ModelException(ExceptionCode.INTERNAL, s"Unexpected $ref binding: ${bind.get}", location)
        }
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

  override protected[xmlcalabash] def propagateStaticBindings(): Unit = {
    val statics = collectStatics(Map.empty[QName,Artifact])
    val staticBindings = mutable.HashMap.empty[Binding, Message]

    for ((name,static) <- statics) {
      static match {
        case variable: Variable =>
          staticBindings.put(static._graphNode.get.asInstanceOf[Binding], variable.staticValueMessage.get)
        case option: OptionDecl =>
          staticBindings.put(static._graphNode.get.asInstanceOf[Binding], option.staticValueMessage.get)
        case _ =>
          throw new RuntimeException("This can't happen; statics isn't variable or option")
      }
    }
    _graphNode.get.staticBindings = staticBindings.toMap

    for (child <- children) {
      child.exposeStatics()
    }
  }

  override def asXML: xml.Elem = {
    val nodes = ListBuffer.empty[xml.Node]
    nodes += xml.Text("\n")
    nodes += xml.Comment("FIXME: implement serialization of inline content")
    nodes += xml.Text("\n")
    new xml.Elem("p", "inline", dump_attr.getOrElse(xml.Null),
      namespaceScope, false, nodes:_*)
  }
}
