package com.xmlcalabash.model.xml

import com.jafpl.graph.{Binding, ContainerStart, Graph, Node}
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.datasource.{DataSource, Document, Empty, Inline, Pipe}
import com.xmlcalabash.runtime.{ExpressionContext, SaxonExpressionOptions, XMLCalabashRuntime, XProcExpression, XProcXPathExpression}
import net.sf.saxon.s9api.QName
import net.sf.saxon.value.SequenceType

class WithOption(override val config: XMLCalabashRuntime,
                 override val parent: Option[Artifact]) extends Artifact(config, parent) {
  private var _collection = false
  private var _name: QName = _
  private var _expression = Option.empty[XProcExpression]
  private var _as = Option.empty[SequenceType]

  def this(config: XMLCalabashRuntime, parent: Artifact, name: QName, expr: XProcExpression) = {
    this(config, Some(parent))
    _name = name
    _expression = Some(expr)
  }

  def optionName: QName = _name
  def expression: XProcExpression = _expression.get
  def as: Option[SequenceType] = _as

  override def validate(): Boolean = {
    var valid = super.validate()

    val qname = lexicalQName(attributes.get(XProcConstants._name))
    if (qname.isEmpty) {
      throw new ModelException(ExceptionCode.NAMEATTRREQ, this.toString, location)
    }

    _name = qname.get

    _collection = lexicalBoolean(attributes.get(XProcConstants._collection)).getOrElse(false)

    _as = sequenceType(attributes.get(XProcConstants._as))

    val selattr = attributes.get(XProcConstants._select)
    if (selattr.isEmpty) {
      throw new ModelException(ExceptionCode.SELECTATTRREQ, this.toString, location)
    } else {
      val context = new ExpressionContext(staticContext)
      _expression = Some(new XProcXPathExpression(context, selattr.get, as))
    }

    val href = attributes.get(XProcConstants._href)
    val pipe = attributes.get(XProcConstants._pipe)

    for (key <- List(XProcConstants._name, XProcConstants._select, XProcConstants._collection,
      XProcConstants._href, XProcConstants._pipe)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    var hasDataSources = false
    var emptyCount = 0
    var nonEmptyCount = 0
    var hasImplicit = false
    var hasExplicit = false
    for (child <- children) {
      child match {
        case ds: DataSource =>
          hasDataSources = true
          valid = valid && child.validate()
          child match {
            case inline: Inline =>
              hasImplicit = hasImplicit || inline.isImplicit
              hasExplicit = hasExplicit || !inline.isImplicit
              nonEmptyCount += 1
            case empty: Empty =>
              emptyCount += 1
              hasExplicit = true
            case _ =>
              nonEmptyCount += 1
              hasExplicit = true
          }
          if (hasImplicit && hasExplicit) {
            throw XProcException.xsElementNotAllowed(child.location, child.nodeName, "cannot mix implicit inlines with elements in the XProc namespace")
          }
        case d: Documentation =>
          hasExplicit = true
          if (hasImplicit) {
            throw XProcException.xsElementNotAllowed(child.location, child.nodeName, "cannot mix implicit inlines with elements in the XProc namespace")
          }
        case p: PipeInfo =>
          hasExplicit = true
          if (hasImplicit) {
            throw XProcException.xsElementNotAllowed(child.location, child.nodeName, "cannot mix implicit inlines with elements in the XProc namespace")
          }
        case _ => throw XProcException.xsElementNotAllowed(location, child.nodeName)
      }
    }

    if ((emptyCount > 0) && ((emptyCount != 1) || (nonEmptyCount != 0))) {
      throw XProcException.xsNoSiblingsOnEmpty(location)
    }

    if (href.isDefined) {
      if (hasDataSources) {
        throw XProcException.staticError(81, href.get, location)
      }
      hasDataSources = true

      for (uri <- href.get.split("\\s+")) {
        val ruri = baseURI.get.resolve(uri)
        val doc = new Document(config, this, ruri.toASCIIString)
        addChild(doc)
      }
    }

    if (pipe.isDefined) {
      if (hasDataSources) {
        throw XProcException.staticError(82, pipe.get, location)
      }
      parsePipeAttribute(pipe.get)
    }

    valid
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val atomic = this.parent.get
    val grandparent = parent.parent.get // parent is atomic step

    grandparent match {
      case start: ContainerStart =>
        _graphNode = Some(start.addVariable(optionName.getClarkName, expression))
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
