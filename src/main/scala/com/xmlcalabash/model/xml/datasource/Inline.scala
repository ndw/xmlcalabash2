package com.xmlcalabash.model.xml.datasource

import com.jafpl.graph.{Binding, ContainerStart, Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.{ParserConfiguration, ValueParser, XProcConstants}
import com.xmlcalabash.model.xml.{Artifact, DeclareStep, IOPort, Input, OptionDecl, Output, Variable, WithInput, WithOption}
import com.xmlcalabash.runtime.ExpressionContext
import com.xmlcalabash.steps.internal.InlineLoader
import net.sf.saxon.s9api.{Axis, QName, XdmNode, XdmNodeKind}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Inline(override val config: XMLCalabash,
             override val parent: Option[Artifact],
             val nodes: List[XdmNode]) extends DataSource(config, parent) {
  private var _excludeInlinePrefixes = Map.empty[String,String]
  private var _expandText = true
  private var _allowExpandText = true
  private var _documentProperties = Option.empty[String]
  private var _encoding = Option.empty[String]
  protected[xml] val variableRefs = mutable.HashSet.empty[QName]

  def this(config: XMLCalabash, parent: Artifact, inline: Inline) {
    this(config, Some(parent), inline.nodes)
    _excludeInlinePrefixes = inline._excludeInlinePrefixes
    _expandText = inline._expandText
    _documentProperties = inline._documentProperties
    _encoding = inline._encoding
    variableRefs.clear()
    variableRefs ++= inline.variableRefs
  }

  protected[xmlcalabash] def allowExpandText: Boolean = _allowExpandText
  protected[xmlcalabash] def allowExpandText_=(allow: Boolean): Unit = {
    _allowExpandText = allow
  }

  override def validate(): Boolean = {
    _excludeInlinePrefixes = lexicalPrefixes(attributes.get(XProcConstants._exclude_inline_prefixes))
    _expandText = lexicalBoolean(attributes.get(XProcConstants._expand_text)).getOrElse(true)
    _documentProperties = attributes.get(XProcConstants._document_properties)
    _encoding = attributes.get(XProcConstants._encoding)

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

    true
  }

  protected[xml] def checkForBindings(): Unit = {
    for (node <- nodes) {
      variableRefs ++= ValueParser.findVariableRefs(config, node, _expandText, location)
      //findVariableRefs(node, _expandText)
      if (_documentProperties.isDefined) {
        variableRefs ++= ValueParser.findVariableRefsInString(config, _documentProperties.get, location)
        //findVariableRefsInString(_documentProperties.get)
      }
    }
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val container = this.parent.get match {
      case wi: WithInput => this.parent.get.parent.get.parent.get
      case xi: Input => this.parent.get.parent.get
      case wo: WithOption => this.parent.get.parent.get.parent.get
      case v: Variable => this.parent.get.parent.get
      case _ => this.parent.get
    }
    val cnode = container._graphNode.get.asInstanceOf[ContainerStart]

    val context = new ExpressionContext(baseURI, inScopeNS, location)
    val produceInline = new InlineLoader(baseURI, nodes, context, _expandText, _excludeInlinePrefixes, _documentProperties, _encoding)

    produceInline.allowExpandText = allowExpandText
    produceInline.location = location
    val inlineProducer = cnode.addAtomic(produceInline)

    _graphNode = Some(inlineProducer)
    config.addNode(inlineProducer.id, this)

    for (ref <- variableRefs) {
      val bind = findBinding(ref)
      if (bind.isEmpty) {
        throw new ModelException(ExceptionCode.NOBINDING, ref.toString, location)
      }

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
          graph.addBindingEdge(optDecl._graphNode.get.asInstanceOf[Binding], inlineProducer)
        case varDecl: Variable =>
          graph.addBindingEdge(varDecl._graphNode.get.asInstanceOf[Binding], inlineProducer)
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
