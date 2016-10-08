package com.xmlcalabash.drivers

import java.io.{FileWriter, PrintStream}

import com.xmlcalabash.calc.{AddExpr, MultExpr, NumberLiteral, UnaryExpr}
import com.xmlcalabash.core.XProcEngine
import com.xmlcalabash.graph.{Graph, InputNode, Node, XProcRuntime}
import com.xmlcalabash.items.NumberItem
import com.xmlcalabash.model.xml.util.RelevantNodes
import com.xmlcalabash.xpath.{CalcParser, XPathParser}
import net.sf.saxon.s9api._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object CalcDemo extends App {
  val processor = new Processor(false)
  val engine = new XProcEngine(processor)
  val graph = new Graph(engine)

  val _MultiplicativeExpr = new QName("", "MultiplicativeExpr")
  val _UnaryExpr = new QName("", "UnaryExpr")
  val _AdditiveExpr = new QName("", "AdditiveExpr")
  val _IntegerLiteral = new QName("", "IntegerLiteral")
  val _TOKEN = new QName("", "TOKEN")
  val _VarRef = new QName("", "VarRef")
  val _nid = new QName("", "nid")

  val multOps = Array("*", "div")
  val addOps = Array("+", "-")

  var bindings = mutable.HashMap.empty[String, Int]
  var expr = ""
  var dumpTree: Option[String] = None
  var dumpSimpleTree: Option[String] = None
  var dumpGraph: Option[String] = None

  var argpos = 0
  while (argpos < args.length) {
    val arg = args(argpos)
    if (arg.startsWith("-t")) {
      dumpTree = Some(arg.substring(2))
    } else if (arg.startsWith("-s")) {
      dumpSimpleTree = Some(arg.substring(2))
    } else if (arg.startsWith("-g")) {
      dumpGraph = Some(arg.substring(2))
    } else if (arg.startsWith("-v")) {
      val bind = arg.substring(2)
      val pos = bind.indexOf("=")
      if (pos > 0) {
        bindings.put(bind.substring(0, pos), bind.substring(pos + 1).toInt)
      } else {
        halt("Unparsable variable binding: " + arg)
      }
    } else {
      expr += " " + arg
    }
    argpos += 1
  }

  println("Evaluate expression: " + expr)
  if (bindings.nonEmpty) {
    println("Where:")
    for (varname <- bindings.keySet) {
      println("\t" + varname + "=" + bindings(varname))
    }
  }

  val calc = new CalcParser(engine, expr)
  if (calc.parseTree.isEmpty) {
    halt("Lexical error in expression: " + expr)
  }

  var ps = System.out
  if (dumpTree.isDefined) {
    if (dumpTree.get != "-") {
      ps = new PrintStream(dumpTree.get)
    }
    val gdump = graph.dump()
    ps.println(calc.parseTree.get.toString)
    ps.close()
  }

  ps = System.out
  if (dumpSimpleTree.isDefined) {
    if (dumpSimpleTree.get != "-") {
      ps = new PrintStream(dumpSimpleTree.get)
    }
    ps.println(calc.simplifiedTree.get.toString)
    ps.close()
  }

  val nodeMap = mutable.HashMap.empty[String, Node]
  val varMap = mutable.HashMap.empty[String, InputNode]
  var firstElement = true
  val output = graph.createOutputNode("OUTPUT")

  makeNodes(calc.simplifiedTree.get)
  makeEdges(calc.simplifiedTree.get)

  val valid = graph.valid()
  if (!valid) {
    halt("Graph isn't valid?")
  }

  ps = System.out
  if (dumpGraph.isDefined) {
    if (dumpGraph.get != "-") {
      ps = new PrintStream(dumpGraph.get)
    }
    ps.println(graph.dump().toString)
    ps.close()
  }

  val runtime = new XProcRuntime(graph)
  runtime.start()

  for (varname <- varMap.keySet) {
    val node = varMap(varname)
    if (bindings.contains(varname)) {
      node.write(new NumberItem(bindings(varname)))
      node.close()
    } else {
      halt("Expression uses unbound variable: " + varname)
    }
  }

  while (runtime.running) {
    Thread.sleep(100)
  }

  var item = output.read()
  while (item.isDefined) {
    println(item.get)
    item = output.read()
  }

  def makeNodes(node: XdmNode): Unit = {
    val children = ListBuffer.empty[XdmNode]
    for (childitem <- RelevantNodes.filter(node, Axis.CHILD)) {
      children += childitem.asInstanceOf[XdmNode]
    }

    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        for (child <- children) { makeNodes(child) }
      case XdmNodeKind.ELEMENT =>
        val nodeId = name(node)
        var gnode: Option[Node] = None

        if (node.getNodeName == _MultiplicativeExpr) {
          val ops = ListBuffer.empty[String]
          ops += chkTok(children(1), multOps)
          var pos = 3
          while (pos < children.size) {
            ops += chkTok(children(pos), multOps)
            pos += 2
          }
          gnode = Some(graph.createNode(nodeId, new MultExpr(ops.toList)))
        } else if (node.getNodeName == _AdditiveExpr) {
          val ops = ListBuffer.empty[String]
          ops += chkTok(children(1), addOps)
          var pos = 3
          while (pos < children.size) {
            ops += chkTok(children(pos), addOps)
            pos += 2
          }
          gnode = Some(graph.createNode(nodeId, new AddExpr(ops.toList)))
        } else if (node.getNodeName == _UnaryExpr) {
          val op = chkTok(children(0), addOps)
          gnode = Some(graph.createNode(nodeId, new UnaryExpr(op)))
        } else if (node.getNodeName == _IntegerLiteral) {
          gnode = Some(graph.createNode(nodeId, new NumberLiteral(node.getStringValue.toInt)))
        } else if (node.getNodeName == _VarRef) {
          if (!varMap.contains(nodeId)) {
            gnode = Some(graph.createInputNode(nodeId))
            varMap.put(node.getStringValue, gnode.get.asInstanceOf[InputNode])
          }
        } else if (node.getNodeName == _TOKEN) {
          // nop
        } else {
          halt("Unexpected node name: " + node.getNodeName)
        }

        if (gnode.isDefined) {
          if (firstElement) {
            graph.addEdge(gnode.get, "result", output, "source")
            firstElement = false
          }
          nodeMap.put(nodeId, gnode.get)
        }

        for (child <- children) { makeNodes(child) }
      case _ => Unit
    }
  }

  def makeEdges(node: XdmNode): Unit = {
    val children = ListBuffer.empty[XdmNode]
    for (childitem <- RelevantNodes.filter(node, Axis.CHILD)) {
      children += childitem.asInstanceOf[XdmNode]
    }

    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        for (child <- children) { makeEdges(child) }
      case XdmNodeKind.ELEMENT =>
        val nodeId = name(node)
        var gnode = nodeMap.get(nodeId)

        if (node.getNodeName == _MultiplicativeExpr
          || node.getNodeName == _AdditiveExpr
        ) {
          var count = 1
          var pos = 0
          while (pos < children.size) {
            val operand = nodeMap(name(children(pos)))
            graph.addEdge(operand, "result", gnode.get, "s" + count)
            count += 1
            pos += 2
          }
        } else if (node.getNodeName == _UnaryExpr) {
          val expr = nodeMap(name(children(1)))
          graph.addEdge(expr, "result", gnode.get, "source")
        }
        for (child <- children) { makeEdges(child) }
      case _ => Unit
    }
  }

  def chkTok(child: XdmNode, tokens: Array[String]): String = {
    if (child.getNodeName == _TOKEN && tokens.contains(child.getStringValue)) {
      child.getStringValue
    } else {
      halt("Unexpected token")
      ""
    }
  }

  def name(node: XdmNode): String = {
    node.getNodeName + "_" + node.getAttributeValue(_nid)
  }

  def halt(msg: String): Unit = {
    println(msg)
    System.exit(0)
  }
}
