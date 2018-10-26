package com.xmlcalabash.model.xml.containers

import com.jafpl.graph.{CatchStart, Graph, Node, TryCatchStart}
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.model.xml.{Artifact, Documentation, PipeInfo}
import com.xmlcalabash.runtime.{StaticContext, StepProxy, XMLCalabashRuntime}
import com.xmlcalabash.steps.ExceptionTranslator
import net.sf.saxon.s9api.QName

import scala.collection.mutable.ListBuffer

class Catch(override val config: XMLCalabashRuntime,
            override val parent: Option[Artifact]) extends Container(config, parent, XProcConstants.p_catch) {
  private val _code = new QName("","code")
  private var codes = ListBuffer.empty[QName]

  override def validate(): Boolean = {
    var valid = super.validate()

    val codeList = attributes.get(_code)
    if (codeList.isDefined) {
      for (code <- codeList.get.split("\\s+")) {
        val qname = ValueParser.parseQName(code, inScopeNS, location)
        codes += qname
      }
    }

    for (key <- List(_code)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    for (child <- relevantChildren) {
      valid = valid && child.validate()
    }

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
    _graphNode = Some(node)
    config.addNode(node.id, this)

    for (child <- children) {
      child.makeGraph(graph, node)
    }
  }

  override def makeEdges(graph: Graph, parentNode: Node) {
    for (output <- outputPorts) {
      graph.addEdge(_graphNode.get, output, parentNode, output)
    }

    for (child <- children) {
      child match {
        case doc: Documentation => Unit
        case pipe: PipeInfo => Unit
        case _ =>
          child.makeEdges(graph, _graphNode.get)
      }
    }

    // Is anyone reading the errors?
    if (graphNode.outputs.contains("errors")) {
      val catchNode = graphNode.asInstanceOf[CatchStart]
      val impl = config.stepImplementation(XProcConstants.cx_exception_translator, location.get)
      val proxy = new StepProxy(config, stepType, impl, None, new StaticContext())
      catchNode.translator = proxy
    }
  }
}
