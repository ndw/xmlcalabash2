package com.xmlcalabash.model.xml

import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.{Binding, ContainerStart, Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.xml.datasource.{Document, Empty, Inline, Pipe}
import com.xmlcalabash.runtime.{XProcAvtExpression, XProcExpression, XProcXPathExpression}
import net.sf.saxon.s9api.QName

import scala.collection.mutable

class WithOption(override val config: XMLCalabash,
                 override val parent: Option[Artifact]) extends Artifact(config, parent) {
  private var _name: QName = _
  private var _dataPort = ""
  private var _expression = Option.empty[XProcExpression]
  private var variableRefs = mutable.HashSet.empty[QName]

  def this(config: XMLCalabash, parent: Artifact, name: QName, expr: XProcExpression) = {
    this(config, Some(parent))
    _name = name
    _expression = Some(expr)
  }

  def optionName: QName = _name
  def expression: XProcExpression = _expression.get
  def dataPort: String = _dataPort

  override def validate(): Boolean = {
    if (_expression.isDefined) {
      // This is a synthetic WithOption constructed from an option shortcut
      _dataPort = "#" + _name.toString + "_" + id
      return true
    }

    val qname = lexicalQName(attributes.get(XProcConstants._name))
    if (qname.isEmpty) {
      throw new ModelException(ExceptionCode.NAMEATTRREQ, this.toString, location)
    }

    _name = qname.get
    val selattr = attributes.get(XProcConstants._select)
    if (selattr.isEmpty) {
      throw new ModelException(ExceptionCode.SELECTATTRREQ, this.toString, location)
    } else {
      _expression = Some(new XProcXPathExpression(inScopeNS, selattr.get))
    }

    _dataPort = "#" + _name.toString + "_" + id

    for (key <- List(XProcConstants._name, XProcConstants._select)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    val okChildren = List(classOf[Empty], classOf[Inline], classOf[Pipe], classOf[Document])
    for (child <- relevantChildren()) {
      if (!okChildren.contains(child.getClass)) {
        throw new ModelException(ExceptionCode.BADCHILD, child.toString, location)
      }
      valid = valid && child.validate()
    }

    valid
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val atomic = this.parent.get
    val grandparent = parent.parent.get // parent is atomic step

    grandparent match {
      case start: ContainerStart =>
        graphNode = Some(start.addVariable(optionName.getClarkName, expression))
      case _ =>
        throw new ModelException(ExceptionCode.INTERNAL, "WithOption grandparent isn't a container???", location)
    }

    for (child <- children) {
      child match {
        case inline: Inline =>
          inline.makeGraph(graph, atomic.graphNode.get)
        case pipe: Pipe =>
          pipe.makeGraph(graph, atomic.graphNode.get)
        case _ =>
          throw new PipelineException("unexpected", "unexpected child: " + child, location)
      }
    }
  }

  override def makeEdges(graph: Graph, parent: Node) {
    var explicit = false
    for (child <- children) {
      child match {
        case doc: Documentation => Unit
        case pipe: PipeInfo => Unit
        case _ =>
          explicit = true
          child.makeEdges(graph, parent)
      }
    }

    if (!explicit) {
      val drp = this.parent.get.defaultReadablePort()
      if (drp.isDefined) {
        drp.get match {
          case out: Output =>
            graph.addEdge(out.parent.get.graphNode.get, out.port.get, graphNode.get, "source")
          case in: Input =>
            graph.addEdge(in.parent.get.graphNode.get, in.port.get, graphNode.get, "source")
          case _ =>
            throw new PipelineException("notimpl", "not implemented reading from: " + drp.get, location)
        }
      }
    }

    graph.addEdge(graphNode.get, "result", this.parent.get.graphNode.get, "#bindings")

    findVariableRefs()
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
          graph.addBindingEdge(optDecl.get.graphNode.get.asInstanceOf[Binding], graphNode.get)
        case varDecl: Variable =>
          graph.addBindingEdge(varDecl.graphNode.get.asInstanceOf[Binding], graphNode.get)
        case _ =>
          throw new ModelException(ExceptionCode.INTERNAL, s"Unexpected $ref binding: ${bind.get}", location)
      }
    }
  }

  private def findVariableRefs(): Unit = {
    expression match {
      case expr: XProcXPathExpression =>
        val parser = config.expressionParser
        parser.parse(expr.expr)
        for (ref <- parser.variableRefs) {
          val qname = new QName("", ref)
          variableRefs += qname
        }
      case expr: XProcAvtExpression =>
        var avt = false
        for (subexpr <- expr.avt) {
          if (avt) {
            val parser = config.expressionParser
            parser.parse(subexpr)
            for (ref <- parser.variableRefs) {
              val qname = new QName("", ref)
              variableRefs += qname
            }
          }
          avt = !avt
        }
      case _ =>
        throw new PipelineException("notimpl", "unknown expression type!", location)
    }
  }


}
