package com.xmlcalabash.model.xml.datasource

import com.jafpl.graph.{Binding, ContainerStart, Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.model.xml.{Artifact, DeclareStep, IOPort, OptionDecl}
import com.xmlcalabash.runtime.{ExpressionContext, XProcAvtExpression}
import com.xmlcalabash.steps.internal.FileLoader
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Document(override val config: XMLCalabash,
               override val parent: Option[Artifact])
  extends DataSource(config, parent) {

  private var _href = Option.empty[String]
  private var _docProps = Option.empty[String]
  private var hrefAvt = List.empty[String]
  private var docPropsAvt = List.empty[String]
  private val bindingRefs = mutable.HashSet.empty[QName]

  override def validate(): Boolean = {
    _href = attributes.get(XProcConstants._href)
    _docProps = attributes.get(XProcConstants._document_properties)

    for (key <- List(XProcConstants._href, XProcConstants._document_properties)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (_href.isEmpty) {
      throw new ModelException(ExceptionCode.ATTRREQ, "href", location)
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    if (children.nonEmpty) {
      throw new ModelException(ExceptionCode.BADCHILD, children.head.toString, location)
    }

    hrefAvt = parseAvt("href", _href.get)
    if (_docProps.isDefined) {
      docPropsAvt = parseAvt("document-properties", _docProps.get)
    }

    true
  }

  private def parseAvt(name: String, expr: String): List[String] = {
    val list = ValueParser.parseAvt(expr)
    if (list.isEmpty) {
      throw new ModelException(ExceptionCode.BADAVT, List(name, expr), location)
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

    list.get
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val container = this.parent.get.parent.get.parent.get
    val cnode = container.graphNode.get.asInstanceOf[ContainerStart]
    val step = new FileLoader()
    val docReader = cnode.addAtomic(step, "document")

    val context = new ExpressionContext(baseURI, inScopeNS, location)
    val hrefBinding = cnode.addVariable("href", new XProcAvtExpression(context, hrefAvt))
    graph.addBindingEdge(hrefBinding, docReader)
    graphNode = Some(docReader)
    config.addNode(docReader.id, this)

    val docPropsBinding =  if (_docProps.isDefined) {
      val docPropsBinding = cnode.addVariable("document-properties", new XProcAvtExpression(context, docPropsAvt))
      graph.addBindingEdge(docPropsBinding, docReader)
      graphNode = Some(docReader)
      Some(docPropsBinding)
    } else {
      None
    }

    for (ref <- bindingRefs) {
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
          graph.addBindingEdge(optDecl.get.graphNode.get.asInstanceOf[Binding], hrefBinding)
          if (docPropsBinding.isDefined) {
            graph.addBindingEdge(optDecl.get.graphNode.get.asInstanceOf[Binding], docPropsBinding.get)
          }

        case _ =>
          throw new ModelException(ExceptionCode.INTERNAL, s"Unexpected $ref binding: ${bind.get}", location)
      }
    }
  }

  override def makeEdges(graph: Graph, parent: Node): Unit = {
    val toStep = this.parent.get.parent
    val toPort = this.parent.get.asInstanceOf[IOPort].port.get
    graph.addOrderedEdge(graphNode.get, "result", toStep.get.graphNode.get, toPort)
  }

  override def asXML: xml.Elem = {
    dumpAttr("href", _href)
    dumpAttr("document-properties", _docProps)

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
