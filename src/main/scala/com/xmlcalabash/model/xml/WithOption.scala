package com.xmlcalabash.model.xml

import com.jafpl.graph.{Binding, ContainerStart, Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.datasource.{Document, Empty, Inline, Pipe}
import com.xmlcalabash.runtime.{ExpressionContext, SaxonExpressionOptions, XProcExpression, XProcXPathExpression}
import net.sf.saxon.expr.parser.XPathParser
import net.sf.saxon.s9api.QName
import net.sf.saxon.sxpath.IndependentContext
import net.sf.saxon.trans.XPathException
import net.sf.saxon.value.SequenceType

class WithOption(override val config: XMLCalabash,
                 override val parent: Option[Artifact]) extends Artifact(config, parent) {
  private var _collection = false
  private var _name: QName = _
  private var _expression = Option.empty[XProcExpression]
  private var _as = Option.empty[SequenceType]

  def this(config: XMLCalabash, parent: Artifact, name: QName, expr: XProcExpression) = {
    this(config, Some(parent))
    _name = name
    _expression = Some(expr)
  }

  def optionName: QName = _name
  def expression: XProcExpression = _expression.get
  def as: Option[SequenceType] = _as

  override def validate(): Boolean = {
    if (_expression.isDefined) {
      return true
    }

    var valid = true

    val qname = lexicalQName(attributes.get(XProcConstants._name))
    if (qname.isEmpty) {
      throw new ModelException(ExceptionCode.NAMEATTRREQ, this.toString, location)
    }

    _name = qname.get

    _collection = lexicalBoolean(attributes.get(XProcConstants._collection)).getOrElse(false)

    val seqType = attributes.get(XProcConstants._as)
    if (seqType.isDefined) {
      try {
        val parser = new XPathParser
        parser.setLanguage(XPathParser.SEQUENCE_TYPE, 31)
        val ic = new IndependentContext(config.processor.getUnderlyingConfiguration)
        for ((prefix, uri) <- inScopeNS) {
          ic.declareNamespace(prefix, uri)
        }
        _as = Some(parser.parseSequenceType(seqType.get, ic))
      } catch {
        case xpe: XPathException =>
          throw XProcException.dynamicError(49, List(seqType.get, xpe.getMessage), location)
        case t: Throwable =>
          throw t
      }
    }

    val selattr = attributes.get(XProcConstants._select)
    if (selattr.isEmpty) {
      throw new ModelException(ExceptionCode.SELECTATTRREQ, this.toString, location)
    } else {
      val context = new ExpressionContext(baseURI, inScopeNS, location)
      _expression = Some(new XProcXPathExpression(context, selattr.get, as))
    }

    for (key <- List(XProcConstants._name, XProcConstants._select, XProcConstants._collection)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    val okChildren = List(classOf[Empty], classOf[Inline], classOf[Pipe], classOf[Document])
    for (child <- relevantChildren) {
      if (!okChildren.contains(child.getClass)) {
        throw XProcException.xsElementNotAllowed(location, child.nodeName)
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
        val options = new SaxonExpressionOptions(Map("collection" -> _collection))
        _graphNode = Some(start.addVariable(optionName.getClarkName, expression, None, options))
      case _ =>
        throw new ModelException(ExceptionCode.INTERNAL, "WithOption grandparent isn't a container???", location)
    }

    for (child <- children) {
      child match {
        case inline: Inline =>
          inline.makeGraph(graph, atomic._graphNode.get)
        case pipe: Pipe =>
          pipe.makeGraph(graph, atomic._graphNode.get)
        case _ =>
          throw new ModelException(ExceptionCode.INTERNAL, s"Unexpected child in p:with-option: $child", location)
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
      val drp = this.parent.get.defaultReadablePort
      if (drp.isDefined) {
        drp.get match {
          case out: Output =>
            graph.addEdge(out.parent.get._graphNode.get, out.port.get, _graphNode.get, "source")
          case in: Input =>
            graph.addEdge(in.parent.get._graphNode.get, in.port.get, _graphNode.get, "source")
          case _ =>
            throw new ModelException(ExceptionCode.INTERNAL, s"Not implemented in p:with-option, reading from ${drp.get}", location)
        }
      }
    }

    graph.addEdge(_graphNode.get, "result", this.parent.get._graphNode.get, "#bindings")

    val variableRefs = findVariableRefs(expression)
    for (ref <- variableRefs) {
      this.parent.get.asInstanceOf[PipelineStep].addVariableRef(ref)

      if (this.parent.get.asInstanceOf[PipelineStep].variableRefs.contains(ref)) {
        this.parent.get.asInstanceOf[PipelineStep].variableRefs -= ref
      }

      val bind = findBinding(ref)
      if (bind.isEmpty) {
        throw new ModelException(ExceptionCode.NOBINDING, ref.toString, location)
      }

      bind.get match {
          /*
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
          graph.addBindingEdge(optDecl.get._graphNode.get.asInstanceOf[Binding], _graphNode.get)
          */
        case optDecl: OptionDecl =>
          graph.addBindingEdge(optDecl._graphNode.get.asInstanceOf[Binding], _graphNode.get)
        case varDecl: Variable =>
          graph.addBindingEdge(varDecl._graphNode.get.asInstanceOf[Binding], _graphNode.get)
        case _ =>
          throw new ModelException(ExceptionCode.INTERNAL, s"Unexpected $ref binding: ${bind.get}", location)
      }
    }
  }
}
