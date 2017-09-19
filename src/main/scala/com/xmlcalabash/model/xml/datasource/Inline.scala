package com.xmlcalabash.model.xml.datasource

import com.jafpl.graph.{Binding, ContainerStart, Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.{ParserConfiguration, ValueParser, XProcConstants}
import com.xmlcalabash.model.xml.{Artifact, DeclareStep, IOPort, OptionDecl, Variable, WithOption}
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
  private var _documentProperties = Option.empty[String]
  private var _encoding = Option.empty[String]
  private val variableRefs = mutable.HashSet.empty[QName]

  override def validate(): Boolean = {
    _excludeInlinePrefixes = lexicalPrefixes(attributes.get(XProcConstants._exclude_inline_prefixes))
    _expandText = lexicalBoolean(attributes.get(XProcConstants._expand_text)).getOrElse(true)
    _documentProperties = attributes.get(XProcConstants._document_properties)
    _encoding = attributes.get(XProcConstants._encoding)

    for (key <- List(XProcConstants._exclude_inline_prefixes, XProcConstants._expand_text,
      XProcConstants._document_properties, XProcConstants._encoding)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      throw new ModelException(ExceptionCode.BADATTR, attributes.keySet.head.toString, location)
    }

    for (node <- nodes) {
      findVariableRefs(node, _expandText)
      if (_documentProperties.isDefined) {
        findVariableRefsInString(_documentProperties.get)
      }
    }

    true
  }

  private def findVariableRefs(node: XdmNode, expandText: Boolean): Unit = {
    node.getNodeKind match {
      case XdmNodeKind.ELEMENT =>
        var newExpand = expandText
        var iter = node.axisIterator(Axis.ATTRIBUTE)
        while (iter.hasNext) {
          val attr = iter.next().asInstanceOf[XdmNode]
          if (expandText) {
            findVariableRefsInAvt(attr.getStringValue)
          }
          if (attr.getNodeName == XProcConstants.p_expand_text) {
            newExpand = lexicalBoolean(Some(attr.getStringValue)).get
          }
        }
        iter = node.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val child = iter.next().asInstanceOf[XdmNode]
          findVariableRefs(child, newExpand)
        }
      case XdmNodeKind.TEXT =>
        if (expandText) {
          findVariableRefsInAvt(node.getStringValue)
        }
      case _ => Unit
    }
  }

  private def findVariableRefsInAvt(text: String): Unit = {
    val list = ValueParser.parseAvt(text)
    if (list.isEmpty) {
      throw new ModelException(ExceptionCode.BADAVT, List("TVT", text), location)
    }

    var avt = false
    for (substr <- list.get) {
      if (avt) {
        findVariableRefsInString(substr)
      }
      avt = !avt
    }
  }

  private def findVariableRefsInString(text: String): Unit = {
    val parser = config.expressionParser
    parser.parse(text)
    for (ref <- parser.variableRefs) {
      val qname = lexicalQName(Some(ref)).get
      variableRefs += qname
    }
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val container = this.parent.get.parent.get.parent.get
    val cnode = container.graphNode.get.asInstanceOf[ContainerStart]

    val context = new ExpressionContext(baseURI, inScopeNS, location)
    val produceInline = new InlineLoader(nodes, context, _expandText, _excludeInlinePrefixes, _documentProperties, _encoding)
    val inlineProducer = cnode.addAtomic(produceInline)

    graphNode = Some(inlineProducer)
    config.addNode(inlineProducer.id, this)

    for (ref <- variableRefs) {
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
          graph.addBindingEdge(optDecl.get.graphNode.get.asInstanceOf[Binding], inlineProducer)
        case varDecl: Variable =>
          graph.addBindingEdge(varDecl.graphNode.get.asInstanceOf[Binding], inlineProducer)
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
        toNode = opt.graphNode
        toPort = "source"
      case port: IOPort =>
        toNode = parent.get.parent.get.graphNode
        toPort = port.port.get
      case _ =>
        throw new ModelException(ExceptionCode.INTERNAL, "p:inline points to " + parent.get, location)
    }

    graph.addOrderedEdge(graphNode.get, "result", toNode.get, toPort)
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
