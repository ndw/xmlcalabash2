package com.xmlcalabash.model.xml.datasource

import com.jafpl.graph.{Binding, ContainerStart, Graph, Node}
import com.xmlcalabash.exceptions.ModelException
import com.xmlcalabash.model.util.{AvtParser, ParserConfiguration}
import com.xmlcalabash.model.xml.{Artifact, DeclareStep, IOPort, OptionDecl, Variable, XProcConstants}
import com.xmlcalabash.steps.ProduceInline
import net.sf.saxon.s9api.{Axis, QName, XdmNode, XdmNodeKind}

import scala.collection.mutable

class Inline(override val config: ParserConfiguration,
             override val parent: Option[Artifact],
             val nodes: List[XdmNode]) extends DataSource(config, parent) {
  private var _excludeInlinePrefixes: Set[String] = Set()
  private var _expandText = true
  private var _documentProperties = Option.empty[String]
  private var _encoding = Option.empty[String]
  private val variableRefs = mutable.HashSet.empty[QName]

  override def validate(): Boolean = {
    _excludeInlinePrefixes = lexicalPrefixes(properties.get(XProcConstants._exclude_inline_prefixes))
    _expandText = lexicalBoolean(properties.get(XProcConstants._expand_text)).getOrElse(true)
    _documentProperties = properties.get(XProcConstants._document_properties)
    _encoding = properties.get(XProcConstants._encoding)

    for (key <- List(XProcConstants._exclude_inline_prefixes, XProcConstants._expand_text,
      XProcConstants._document_properties, XProcConstants._encoding)) {
      if (properties.contains(key)) {
        properties.remove(key)
      }
    }

    if (properties.nonEmpty) {
      throw new ModelException("badopt",
        s"Unexpected attribute: ${properties.keySet.head.getLocalName}", location)
    }

    for (node <- nodes) {
      findVariableRefs(node, _expandText)
    }

    valid
  }

  private def findVariableRefs(node: XdmNode, expandText: Boolean): Unit = {
    node.getNodeKind match {
      case XdmNodeKind.ELEMENT =>
        var newExpand = expandText
        var iter = node.axisIterator(Axis.ATTRIBUTE)
        while (iter.hasNext) {
          val attr = iter.next().asInstanceOf[XdmNode]
          if (expandText) {
            findVariableRefsInString(attr.getStringValue)
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
          findVariableRefsInString(node.getStringValue)
        }
      case _ => Unit
    }
  }

  private def findVariableRefsInString(text: String): Unit = {
    val list = AvtParser.parse(text)
    if (list.isEmpty) {
      throw new ModelException("badexpr", s"Error in AVT: $text", location)
    }

    var avt = false
    for (substr <- list.get) {
      if (avt) {
        val parser = config.expressionParser
        parser.parse(substr)
        for (ref <- parser.variableRefs) {
          val qname = lexicalQName(Some(ref)).get
          variableRefs += qname
        }
      }
      avt = !avt
    }
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val container = this.parent.get.parent.get.parent.get
    val cnode = container.graphNode.get.asInstanceOf[ContainerStart]
    val produceInline = new ProduceInline(nodes, _expandText)
    val inlineProducer = cnode.addAtomic(produceInline)
    graphNode = Some(inlineProducer)

    for (ref <- variableRefs) {
      val bind = findBinding(ref)
      if (bind.isEmpty) {
        throw new ModelException("nobind", s"No binding for $ref", location)
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
            throw new ModelException("nobind", s"No binding for $ref", location)
          }
          graph.addBindingEdge(optDecl.get.graphNode.get.asInstanceOf[Binding], inlineProducer)
        case varDecl: Variable =>
          graph.addBindingEdge(varDecl.graphNode.get.asInstanceOf[Binding], inlineProducer)
        case _ =>
          throw new ModelException("unexpected", s"Unexpected $ref binding: ${bind.get}", location)
      }
    }
  }

  override def makeEdges(graph: Graph, parent: Node): Unit = {
    val toStep = this.parent.get.parent
    val toPort = this.parent.get.asInstanceOf[IOPort].port.get
    graph.addEdge(graphNode.get, "result", toStep.get.graphNode.get, toPort)
  }
}
