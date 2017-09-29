package com.xmlcalabash.model.xml

import com.jafpl.graph.{Binding, Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.containers.Container
import com.xmlcalabash.model.xml.datasource.{DataSource, Pipe}
import com.xmlcalabash.runtime.{ExpressionContext, XProcExpression, XProcXPathExpression}
import net.sf.saxon.s9api.QName

import scala.collection.mutable.ListBuffer

class Input(override val config: XMLCalabash,
            override val parent: Option[Artifact]) extends IOPort(config, parent) {
  protected val _pipe = new QName("", "pipe")
  protected var _select: Option[String] = None
  protected var _expression = Option.empty[XProcExpression]

  protected[xml] def this(config: XMLCalabash, parent: Artifact, port: String, primary: Boolean, sequence: Boolean) {
    this(config, Some(parent))
    _port = Some(port)
    _primary = Some(primary)
    _sequence = Some(sequence)
  }

  def select: Option[String] = _select
  def selectExpression: XProcExpression = _expression.get

  override def validate(): Boolean = {
    var valid = super.validate()

    _port = attributes.get(XProcConstants._port)
    _select = attributes.get(XProcConstants._select)
    if (_select.isDefined) {
      val context = new ExpressionContext(baseURI, inScopeNS, location)
      _expression = Some(new XProcXPathExpression(context, _select.get))
    }

    var attr = attributes.get(XProcConstants._primary)
    if (attr.isDefined) {
      attr.get match {
        case "true" => _primary = Some(true)
        case "false" => _primary = Some(false)
        case _ => throw new RuntimeException("primary must be true or false")
      }
    } else {
      _primary = None
    }

    attr = attributes.get(XProcConstants._sequence)
    if (attr.isDefined) {
      attr.get match {
        case "true" => _sequence = Some(true)
        case "false" => _sequence = Some(false)
        case _ => throw new RuntimeException("sequence must be true or false")
      }
    } else {
      _sequence = None
    }

    val pipe = attributes.get(_pipe)

    for (key <- List(XProcConstants._port, XProcConstants._sequence, XProcConstants._primary, XProcConstants._select, _pipe)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    var hasDataSources = false
    if (parent.isDefined && parent.get.isInstanceOf[Container]) {
      for (child <- children) {
        child match {
          case ds: DataSource =>
            hasDataSources = true
            if (child.isInstanceOf[Pipe]) {
              throw new ModelException(ExceptionCode.BADPIPE, this.toString, location)
            }
            valid = valid && child.validate()
          case doc: Documentation => Unit
          case info: PipeInfo => Unit
          case _ =>
            throw new ModelException(ExceptionCode.BADCHILD, child.toString, location)
        }
      }
    } else {
      if (_sequence.isDefined) {
        throw new ModelException(ExceptionCode.BADSEQ, "sequence", location)
      }
      if (_primary.isDefined) {
        throw new ModelException(ExceptionCode.BADPRIMARY, "primary", location)
      }
      for (child <- children) {
        if (dataSourceClasses.contains(child.getClass)) {
          hasDataSources = true
          valid = valid && child.validate()
        } else {
          throw new ModelException(ExceptionCode.BADCHILD, child.toString, location)
        }
      }
    }

    if (pipe.isDefined) {
      if (hasDataSources) {
        throw new ModelException(ExceptionCode.MIXEDPIPE, pipe.get, location)
      }
      for (spec <- pipe.get.split("\\s+")) {
        val pos = spec.indexOf("@")
        if (pos > 0) {
          val step = spec.substring(0, pos)
          val port = spec.substring(pos + 1)
          val pipe = if (step == "") {
            new Pipe(config, this, None, Some(port))
          } else {
            new Pipe(config, this, Some(step), Some(port))
          }
          addChild(pipe)
        } else {
          val pipe = new Pipe(config, this, spec)
          addChild(pipe)
        }
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
      val graphNode = this.parent.get.graphNode
      val context = new ExpressionContext(baseURI, inScopeNS, location)
      val expression = new XProcXPathExpression(context, select.get)
      val variableRefs = findVariableRefs(expression)
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
