package com.xmlcalabash.model.xml.containers

import com.jafpl.graph.{Graph, Node, TryCatchStart}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.model.xml.{Artifact, Documentation, PipeInfo}
import com.xmlcalabash.runtime.{ExpressionContext, XProcXPathExpression}
import net.sf.saxon.s9api.QName

import scala.collection.mutable.ListBuffer

class Catch(override val config: XMLCalabash,
            override val parent: Option[Artifact]) extends Container(config, parent) {
  private val _code = new QName("","code")
  private var codes = ListBuffer.empty[QName]

  override def validate(): Boolean = {
    var valid = true

    _name = attributes.get(XProcConstants._name)
    if (_name.isDefined) {
      label = _name.get
    } else {
      label = "when"
    }

    val codeList = attributes.get(_code)
    if (codeList.isDefined) {
      for (code <- codeList.get.split("\\s+")) {
        val qname = ValueParser.parseQName(code, inScopeNS)
        codes += qname
      }
    }

    for (key <- List(XProcConstants._name, _code)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    for (child <- relevantChildren()) {
      valid = valid && child.validate()
    }

    valid = valid && makePortsExplicit()
    valid = valid && makeBindingsExplicit()

    valid
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val node = parent match {
      case trycatch: TryCatchStart =>
        if (codes.isEmpty) {
          trycatch.addCatch(name)
        } else {
          trycatch.addCatch(name, codes.toList)
        }
      case _ =>
        throw new ModelException(ExceptionCode.INTERNAL, "Catch parent isn't a try/catch???", location)
    }
    graphNode = Some(node)

    for (child <- children) {
      child.makeGraph(graph, node)
    }
  }

  override def makeEdges(graph: Graph, parentNode: Node) {
    for (output <- outputPorts) {
      graph.addEdge(graphNode.get, output, parentNode, output)
    }

    for (child <- children) {
      child match {
        case doc: Documentation => Unit
        case pipe: PipeInfo => Unit
        case _ =>
          child.makeEdges(graph, graphNode.get)
      }
    }
  }
}
