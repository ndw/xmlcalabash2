package com.xmlcalabash.model.xml

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.datasource.{DataSource, Document, Pipe}
import com.xmlcalabash.runtime.{ExpressionContext, XProcExpression, XProcXPathExpression}
import com.xmlcalabash.util.MediaType

import scala.collection.mutable.ListBuffer

class Input(override val config: XMLCalabash,
            override val parent: Option[Artifact]) extends IOPort(config, parent) {
  protected var _select: Option[String] = None
  protected var _expression = Option.empty[XProcExpression]
  protected var _contentTypes = ListBuffer.empty[MediaType]
  protected var _defaultInputs = ListBuffer.empty[DataSource]

  protected[xml] def this(config: XMLCalabash, parent: Artifact, port: String, primary: Boolean, sequence: Boolean) {
    this(config, Some(parent))
    _port = Some(port)
    _primary = Some(primary)
    _sequence = Some(sequence)
  }

  def select: Option[String] = _select
  def selectExpression: XProcExpression = _expression.get

  protected[xml] def manageDefaultInputs(): Unit = {
    val newChildren = ListBuffer.empty[Artifact]
    for (child <- children) {
      child match {
        case ds: DataSource => _defaultInputs += ds
        case _ => newChildren += child
      }
    }

    children.clear()
    for (child <- newChildren) {
      children += child
    }
  }

  def contentTypes(): List[MediaType] = {
    _contentTypes.toList
  }

  def defaultInputs(): List[DataSource] = {
    _defaultInputs.toList
  }

  override def validate(): Boolean = {
    var valid = super.validate()

    _port = attributes.get(XProcConstants._port)
    _select = attributes.get(XProcConstants._select)
    if (_select.isDefined) {
      val context = new ExpressionContext(baseURI, inScopeNS, location)
      _expression = Some(new XProcXPathExpression(context, _select.get))
    }

    _primary = lexicalBoolean(attributes.get(XProcConstants._primary))
    _sequence = lexicalBoolean(attributes.get(XProcConstants._sequence))

    val href = attributes.get(XProcConstants._href)

    val ctypes = attributes.get(XProcConstants._content_types)
    if (ctypes.isDefined) {
      _contentTypes ++= MediaType.parseList(ctypes.get)
    } else {
      _contentTypes += MediaType.ANY
    }

    for (key <- List(XProcConstants._port, XProcConstants._sequence, XProcConstants._primary,
      XProcConstants._select, XProcConstants._pipe, XProcConstants._href, XProcConstants._content_types)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    // N.B. This is an *input*, not a *with-input*, so the data sources are now
    // actually in the _defaultInputs list, not the children!

    var hasDataSources = false
    for (ds <- defaultInputs()) {
      hasDataSources = true
      if (ds.isInstanceOf[Pipe]) {
        throw new ModelException(ExceptionCode.BADPIPE, this.toString, location)
      }
      valid = valid && ds.validate()
    }

    if (href.isDefined) {
      if (hasDataSources) {
        throw XProcException.staticError(81, href.get, location)
      }
      hasDataSources = true

      for (uri <- href.get.split("\\s+")) {
        val doc = new Document(config, this, uri)
        _defaultInputs += doc
      }
    }

    valid
  }

  override def makeGraph(graph: Graph, parent: Node) {
    // Process the children in the context of our parent
    for (child <- children) {
      child.makeGraph(graph, parent)
    }
  }

  override def makeEdges(graph: Graph, parent: Node) {
    for (child <- children) {
      child match {
        case doc: Documentation => Unit
        case pipe: PipeInfo => Unit
        case _ =>
          child.makeEdges(graph, parent)
      }
    }

    if (select.isDefined) {
      val graphNode = this.parent.get._graphNode
      val context = new ExpressionContext(baseURI, inScopeNS, location)
      val expression = new XProcXPathExpression(context, select.get)
      val variableRefs = findVariableRefs(expression)
      for (ref <- variableRefs) {
        this.parent.get.asInstanceOf[PipelineStep].addVariableRef(ref)
        /*
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
            graph.addBindingEdge(optDecl.get._graphNode.get.asInstanceOf[Binding], graphNode.get)
          case varDecl: Variable =>
            graph.addBindingEdge(varDecl._graphNode.get.asInstanceOf[Binding], graphNode.get)
          case _ =>
            throw new ModelException(ExceptionCode.INTERNAL, s"Unexpected $ref binding: ${bind.get}", location)
        }
        */
      }
    }
  }

  override def asXML: xml.Elem = {
    dumpAttr("port", _port)
    dumpAttr("sequence", _sequence)
    dumpAttr("primary", _primary)
    dumpAttr("id", id.toString)

    val nodes = ListBuffer.empty[xml.Node]
    if (children.nonEmpty) {
      nodes += xml.Text("\n")
    }
    for (child <- children) {
      nodes += child.asXML
      nodes += xml.Text("\n")
    }
    new xml.Elem("p", "input", dump_attr.getOrElse(xml.Null),
      namespaceScope, false, nodes:_*)
  }

}
