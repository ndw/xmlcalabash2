package com.xmlcalabash.model.xml

import com.jafpl.graph.{Graph, Node, WhenStart}
import com.xmlcalabash.core.{XProcConstants, XProcEngine}
import com.xmlcalabash.model.xml.bindings.Pipe
import com.xmlcalabash.model.xml.util.WhenOrOtherwise
import com.xmlcalabash.runtime.{XPathExpression, XProcWhenStep}
import com.xmlcalabash.xpath.XPathParser
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/4/16.
  */
class When(node: Option[XdmNode], parent: Option[Artifact]) extends WhenOrOtherwise(node, parent) {
  private[xml] var whenStart: WhenStart = _
  private[xml] var xpathExpr: Node = _
  private var test = ""
  private var test2: Option[String] = None
  protected var _nameRefs: Option[mutable.ListBuffer[QName]] = _
  protected var _funcRefs: Option[mutable.ListBuffer[QName]] = _

  def nameRefs: List[QName] = {
    if (_nameRefs.isDefined) {
      _nameRefs.get.toList
    } else {
      List.empty[QName]
    }
  }

  def funcRefs: List[QName] = {
    if (_funcRefs.isDefined) {
      _funcRefs.get.toList
    } else {
      List.empty[QName]
    }
  }

  def inScopeNamespaces: Map[String, String] = {
    val bindings = mutable.HashMap.empty[String, String]
    var ctx: Option[Artifact] = Some(this)
    while (ctx.isDefined) {
      for (pfx <- ctx.get.nsbindings.keySet) {
        if (!bindings.contains(pfx)) {
          bindings.put(pfx, ctx.get.nsbindings(pfx))
        }
      }
      ctx = ctx.get.parent
    }
    bindings.toMap
  }

  override def makeInputsOutputsExplicit(): Unit = {
    super.makeInputsOutputsExplicit()

    var ctx = children.collect { case ctx: XPathContext => ctx }

    if (ctx.isEmpty) {
      val newChildren = ListBuffer.empty[Artifact]
      val ctx = new XPathContext(None, Some(this))
      newChildren += ctx
      newChildren ++= _children
      _children.clear()
      _children ++= newChildren
    }

    super.makeInputsOutputsExplicit()
  }

  override def buildNodes(graph: Graph, engine: XProcEngine, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    val expr = new XPathExpression(engine, Map.empty[String,String], "boolean(" + test + ")")
    expr.label = "test"
    xpathExpr = graph.createVariableNode(expr)

    val subpipeline = ListBuffer.empty[Node]

    for (child <- children) {
      child.buildNodes(graph, engine, nodeMap)
      child match {
        case ctx: XPathContext => Unit
        case out: Output => Unit
        case log: Log => Unit
        case art: Artifact =>
          subpipeline += nodeMap(art)
      }
    }

    val when = new XProcWhenStep(node.get.getAttributeValue(XProcConstants._select))
    whenStart = graph.createWhenNode(when, subpipeline.toList)
  }

  override private[xml] def buildEdges(graph: Graph, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    val chooseEnd = parent.get.asInstanceOf[Choose].chooseStart._chooseEnd
    val whenEnd = whenStart._whenEnd

    /*
    for (name <- nameRefs) {
      val namedecl = parent.get.parent.get.findNameDecl(name, parent.get)
      var destPort = name.getClarkName
      if (!destPort.startsWith("{")) {
        destPort = "{}" + destPort
      }
      graph.addEdge(nodeMap(namedecl.get), "result", whenStart, destPort)

      println(namedecl)
    }
    */


    for (child <- children) {
      child.buildEdges(graph, nodeMap)

      // Link all of the outputs to whenEnd and link all of the whenEnd outputs to chooseEnd!
      child match {
        case o: Output =>
          for (ochild <- o.children) {
            ochild match {
              case p: Pipe =>
                val srcNode = nodeMap(p._port.get.parent.get)
                //graph.addEdge(srcNode, o.port, whenEnd, "I_" + o.port)
                graph.addEdge(whenEnd, o.port, chooseEnd, "I_" + o.port)
              case _ => Unit
            }
          }
        case ctx: XPathContext =>
          for (pipe <- ctx.children.collect { case pipe: Pipe => pipe }) {
            graph.addEdge(nodeMap(pipe._port.get.parent.get), pipe._port.get.port, xpathExpr, "source")
            graph.addEdge(xpathExpr, "result", whenStart, "condition")
          }

        case _ => Unit
      }
    }
  }

  /*
    override private[xml] def buildEdges(graph: Graph, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    parent.get match {
      case when: When =>
        for (pipe <- children.collect { case pipe: Pipe => pipe }) {
          graph.addEdge(nodeMap(pipe._port.get.parent.get), pipe._port.get.port, when.whenStart, "condition")
        }
      case _ => println("WHAT?")
    }
  }

   */

  override private[model] def parseAttributes(node: XdmNode): Unit = {
    super.parseAttributes(node)

    _funcRefs = Some(mutable.ListBuffer.empty[QName])
    _nameRefs = Some(mutable.ListBuffer.empty[QName])

    val select = property(XProcConstants._test)
    if (select.isDefined) {
      test = select.get.value
      test2 = Some(test)

      val xpp = new XPathParser(test)
      if (xpp.errors) {
        _valid = false
        logger.info("Lexical error in XPath expression: " + test)
      }

      for (lexqname <- xpp.variableRefs()) {
        val qname = new QName(lexqname, node)
        _nameRefs.get += qname
      }

      for (lexqname <- xpp.functionRefs()) {
        val qname = new QName(lexqname, node)
        _funcRefs.get += qname
      }
    }
  }
}
