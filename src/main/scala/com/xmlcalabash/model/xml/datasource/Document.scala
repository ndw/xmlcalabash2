package com.xmlcalabash.model.xml.datasource

import com.jafpl.graph.{Binding, ContainerStart, Graph, Node}
import com.xmlcalabash.exceptions.ModelException
import com.xmlcalabash.model.util.{AvtParser, ParserConfiguration}
import com.xmlcalabash.model.xml.{Artifact, DeclareStep, IOPort, OptionDecl, XProcConstants}
import com.xmlcalabash.runtime.{XProcAvtExpression, XProcXPathExpression}
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Document(override val config: ParserConfiguration,
               override val parent: Option[Artifact])
  extends DataSource(config, parent) {

  private var _href: Option[String] = None
  private val hrefAvt = ListBuffer.empty[String]
  private val bindingRefs = mutable.HashSet.empty[QName]

  override def validate(): Boolean = {
    _href = properties.get(XProcConstants._href)

    for (key <- List(XProcConstants._href)) {
      if (properties.contains(key)) {
        properties.remove(key)
      }
    }

    if (_href.isEmpty) {
      throw new ModelException("hrefreq", "Href is required", location)
    }

    if (properties.nonEmpty) {
      val key = properties.keySet.head
      throw new ModelException("badopt", s"Unexpected attribute: ${key.getLocalName}", location)
    }

    if (children.nonEmpty) {
      throw new ModelException("badelem", s"Unexpected element: ${children.head}", location)
    }

    val list = AvtParser.parse(_href.get)
    if (list.isEmpty) {
      throw new ModelException("badexpr", s"Error in AVT: ${_href.get}", location)
    } else {
      for (item <- list.get) {
        hrefAvt += item
      }
    }

    var avt = false
    for (substr <- list.get) {
      if (avt) {
        val parser = config.expressionParser
        parser.parse(substr)
        for (ref <- parser.variableRefs) {
          val qname = lexicalQName(Some(ref)).get
          bindingRefs += qname
        }
      }
      avt = !avt
    }

    valid
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val container = this.parent.get.parent.get.parent.get
    val cnode = container.graphNode.get.asInstanceOf[ContainerStart]
    val docReader = cnode.addAtomic(config.stepImplementation(XProcConstants.cx_document))

    val hrefBinding = if (hrefAvt.size > 1) {
      cnode.addVariable("href", new XProcAvtExpression(inScopeNS, hrefAvt.toList))
    } else {
      cnode.addVariable("href", new XProcXPathExpression(inScopeNS, _href.get))
    }

    graph.addBindingEdge(hrefBinding, docReader)
    graphNode = Some(docReader)

    for (ref <- bindingRefs) {
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
          graph.addBindingEdge(optDecl.get.graphNode.get.asInstanceOf[Binding], hrefBinding)

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
