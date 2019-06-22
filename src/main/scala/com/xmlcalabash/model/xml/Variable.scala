package com.xmlcalabash.model.xml

import com.jafpl.graph.{Binding, ContainerStart, Graph, Node}
import com.jafpl.messages.Message
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.datasource.{DataSource, Document, Empty}
import com.xmlcalabash.runtime.{ExpressionContext, SaxonExpressionOptions, XMLCalabashRuntime, XProcExpression, XProcXPathExpression}
import net.sf.saxon.s9api.QName
import net.sf.saxon.value.SequenceType

import scala.collection.mutable

class Variable(override val config: XMLCalabashRuntime,
               override val parent: Option[Artifact]) extends Artifact(config, parent) {
  private var _name: QName = new QName("", "UNINITIALIZED")
  private var _collection: Boolean = false
  private var _select = Option.empty[String]
  private var _expression = Option.empty[XProcExpression]
  private var _as = Option.empty[SequenceType]
  private var _static = false
  private var _staticValueMessage = Option.empty[XdmValueItemMessage]

  def variableName: QName = _name
  def select: Option[String] = _select
  def expression: XProcExpression = _expression.get
  def as: Option[SequenceType] = _as

  def static: Boolean = _static

  def staticValueMessage: Option[XdmValueItemMessage] = {
    if (_static && _staticValueMessage.isDefined) {
      _staticValueMessage
    } else {
      None
    }
  }
  protected[model] def staticValueMessage_=(msg: XdmValueItemMessage): Unit = {
    _staticValueMessage = Some(msg)
  }

  override def validate(): Boolean = {
    var valid = super.validate()

    val qname = lexicalQName(attributes.get(XProcConstants._name))
    if (qname.isEmpty) {
      throw new ModelException(ExceptionCode.NAMEATTRREQ, this.toString, location)
    }
    _name = qname.get

    _select = attributes.get(XProcConstants._select)
    _static = lexicalBoolean(attributes.get(XProcConstants._static)).getOrElse(false)
    _collection = lexicalBoolean(attributes.get(XProcConstants._collection)).getOrElse(false)
    if (_select.isEmpty) {
      throw XProcException.xsNoSelectOnVariable(location)
    }

    val pipe = attributes.get(XProcConstants._pipe)
    val href = attributes.get(XProcConstants._href)

    _as = sequenceType(attributes.get(XProcConstants._as))

    for (key <- List(XProcConstants._name, XProcConstants._required, XProcConstants._as,
      XProcConstants._select, XProcConstants._pipe, XProcConstants._href, XProcConstants._collection,
      XProcConstants._static)) {
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
    for (child <- children) {
      child match {
        case ds: DataSource =>
          hasDataSources = true
          valid = valid && child.validate()
          child match {
            case empty: Empty => emptyCount += 1
            case _ => nonEmptyCount += 1
          }
        case d: Documentation => Unit
        case p: PipeInfo => Unit
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
    val container = this.parent.get
    val cnode = container._graphNode.get.asInstanceOf[ContainerStart]
    val options = new SaxonExpressionOptions(Map("collection" -> _collection))

    if (static) {
      val node = cnode.addStaticVariable(_name.getClarkName, options)
      _graphNode = Some(node)
      config.addNode(node.id, this)
    } else {
      val context = new ExpressionContext(staticContext)
      _expression = Some(new XProcXPathExpression(context, _select.get, as))
      val node = cnode.addVariable(_name.getClarkName, expression, options)
      _graphNode = Some(node)
      config.addNode(node.id, this)

      for (child <- children) {
        child.makeGraph(graph, parent)
      }
    }
  }

  override def makeEdges(graph: Graph, parent: Node): Unit = {
    if (!static) {
      var explicitBinding = false
      for (child <- children) {
        child match {
          case doc: Documentation => Unit
          case pipe: PipeInfo => Unit
          case _ =>
            child.makeEdges(graph, parent)
            explicitBinding = true
        }
      }

      if (!explicitBinding) {
        val drp = defaultReadablePort
        if (drp.isDefined) {
          val src = drp.get.parent.get
          graph.addEdge(src._graphNode.get, drp.get.port.get, _graphNode.get, "source")
        }
      }

      val variableRefs = findVariableRefs(expression)
      val statics = collectStatics(Map.empty[QName,Artifact])
      for (ref <- variableRefs) {
        this.parent.get.asInstanceOf[PipelineStep].addVariableRef(ref)
        val bind = findBinding(ref)
        if (bind.isDefined) {
          graph.addBindingEdge(bind.get._graphNode.get.asInstanceOf[Binding], _graphNode.get)
        }
      }
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
}
