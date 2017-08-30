package com.xmlcalabash.model.xml.datasource

import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.xmlcalabash.exceptions.ModelException
import com.xmlcalabash.model.util.ParserConfiguration
import com.xmlcalabash.model.xml.{Artifact, IOPort, XProcConstants}

import scala.collection.mutable.ListBuffer

class Document(override val config: ParserConfiguration,
               override val parent: Option[Artifact]) extends DataSource(config, parent) {
  private var _href: Option[String] = None

  override def validate(): Boolean = {
    _href = properties.get(XProcConstants._href)

    for (key <- List(XProcConstants._href)) {
      if (properties.contains(key)) {
        properties.remove(key)
      }
    }

    if (_href.isEmpty) {
      throw new ModelException("hrefreq", "Href is required")
    }

    if (properties.nonEmpty) {
      val key = properties.keySet.head
      throw new ModelException("badopt", s"Unexpected attribute: ${key.getLocalName}")
    }

    if (children.nonEmpty) {
      throw new ModelException("badelem", s"Unexpected element: ${children.head}")
    }

    valid
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val container = this.parent.get.parent.get.parent.get
    val cnode = container.graphNode.get.asInstanceOf[ContainerStart]
    val hrefBinding = cnode.addVariable("href", _href.get)
    val docReader = cnode.addAtomic(config.stepImplementation(XProcConstants.p_document))
    graph.addBindingEdge(hrefBinding, docReader)
    graphNode = Some(docReader)
  }

  override def makeEdges(graph: Graph, parent: Node): Unit = {
    val toStep = this.parent.get.parent
    val toPort = this.parent.get.asInstanceOf[IOPort].port.get
    graph.addEdge(graphNode.get, "result", toStep.get.graphNode.get, toPort)
  }


  override def asXML: xml.Elem = {
    dumpAttr("href", _href)

    val nodes = ListBuffer.empty[xml.Node]
    nodes += xml.Text("\n")
    for (child <- children) {
      nodes += child.asXML
      nodes += xml.Text("\n")
    }
    new xml.Elem("p", "document", dump_attr.getOrElse(xml.Null),
      namespaceScope, false, nodes:_*)
  }

}
