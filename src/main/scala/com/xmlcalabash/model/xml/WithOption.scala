package com.xmlcalabash.model.xml

import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.ParserConfiguration
import com.xmlcalabash.model.xml.datasource.{Data, Document, Empty, Inline, Pipe}
import net.sf.saxon.s9api.QName

class WithOption(override val config: ParserConfiguration,
                 override val parent: Option[Artifact]) extends Artifact(config, parent) {
  private var _name: QName = new QName("", "UNINITIALIZED")
  private var _select = ""
  private var _dataPort = ""

  def optionName: QName = _name
  def select: String = _select
  def dataPort: String = _dataPort

  override def validate(): Boolean = {
    val qname = lexicalQName(attributes.get(XProcConstants._name))
    if (qname.isEmpty) {
      throw new ModelException(ExceptionCode.NAMEATTRREQ, this.toString, location)
    }

    _name = qname.get
    val selattr = attributes.get(XProcConstants._select)

    if (selattr.isEmpty) {
      throw new ModelException(ExceptionCode.SELECTATTRREQ, this.toString, location)
    } else {
      _select = selattr.get
    }

    for (key <- List(XProcConstants._name, XProcConstants._select)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    _dataPort = "#" + _name.toString + "_" + id

    val okChildren = List(classOf[Empty], classOf[Inline], classOf[Pipe], classOf[Document], classOf[Data])
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
    graphNode = atomic.graphNode

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
    for (child <- children) {
      child match {
        case doc: Documentation => Unit
        case pipe: PipeInfo => Unit
        case _ =>
          child.makeEdges(graph, parent)
      }
    }
  }

}
