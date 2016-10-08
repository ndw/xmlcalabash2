package com.xmlcalabash.drivers

import java.io.FileWriter

import com.xmlcalabash.calc.{AddExpr, NumberLiteral}
import com.xmlcalabash.core.XProcEngine
import com.xmlcalabash.graph.{Graph, XProcRuntime}
import com.xmlcalabash.items.{NumberItem, StringItem}
import com.xmlcalabash.runtime.{Identity, Interleave, XPathExpression}
import net.sf.saxon.s9api.Processor

object GraphTest extends App {
  val processor = new Processor(false)
  val engine = new XProcEngine(processor)

  val graph = new Graph(engine)

  nodes010(graph)

  val valid = graph.valid()
  println(valid)

  if (valid) {
    val pgw = new FileWriter("pg.xml")
    val gdump = graph.dump()
    pgw.write(gdump.toString)
    pgw.close()
    println(gdump)
  }

  def nodes000(graph: Graph): Unit = {
    val a = graph.createNode("a")
  }

  def nodes001(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")

    graph.addEdge(a, "output", b, "input")
  }

  def nodes002(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")
    val c = graph.createNode("c")

    graph.addEdge(a, "output", b, "input")
    graph.addEdge(b, "output", c, "input")
  }

  def nodes003(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")
    val c = graph.createNode("c")

    graph.addEdge(a, "output", b, "input")
    graph.addEdge(b, "input", c, "input")
  }

  def nodes004(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")
    val c = graph.createNode("c")

    graph.addEdge(a, "output", c, "input")
    graph.addEdge(b, "output", c, "input")
  }

  def nodes005(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")
    val c = graph.createNode("c")
    val d = graph.createNode("d")

    graph.addEdge(a, "output", d, "input")
    graph.addEdge(b, "output", d, "input")
    graph.addEdge(c, "output", d, "input")
  }

  def nodes006(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")
    val c = graph.createNode("c")

    graph.addEdge(a, "output", b, "input")
    graph.addEdge(a, "output", c, "input")
  }

  def nodes007(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")
    val c = graph.createNode("c")
    val d = graph.createNode("d")

    graph.addEdge(a, "output", b, "input")
    graph.addEdge(a, "output", c, "input")
    graph.addEdge(a, "output", d, "input")
  }

  def nodes008(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")

    graph.addEdge(a, "output", b, "input")
    graph.addEdge(a, "output", b, "input")
  }

  def nodes009(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")

    graph.addEdge(a, "output", b, "input")
    graph.addEdge(a, "output", b, "input")
    graph.addEdge(a, "output", b, "input")
  }

  def nodes010(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")
    val c = graph.createNode("c")

    graph.addEdge(a, "output", b, "source")
    graph.addEdge(b, "current", c, "input")
  }

  def testGraph1(): Unit = {
    val nStart = graph.createInputNode("nStart")
    val nB1 = graph.createNode("nB1", new Identity("nB1"))
    val nB2 = graph.createNode("nB2", new Identity("NB2"))
    val nEnd = graph.createOutputNode("nEnd")

    graph.addEdge(nStart, "result", nB1, "source")
    graph.addEdge(nStart, "result", nB2, "source")
    graph.addEdge(nB1, "result", nEnd, "source")
    graph.addEdge(nB2, "result", nEnd, "source")
    graph.addDependency(nB2, nB1)

    val valid = graph.valid()
    println(valid)

    if (valid) {
      val runtime = new XProcRuntime(graph)
      runtime.start()

      nStart.write(new StringItem("hello"))
      nStart.write(new StringItem("hello"))
      nStart.close()

      while (runtime.running) {
        Thread.sleep(100)
      }

      var item = nEnd.read()
      while (item.isDefined) {
        println("OUTPUT: " + item.get)
        item = nEnd.read()
      }
    }
  }

  def testGraph2(): Unit = {
    val nStart1 = graph.createInputNode("nStart1")
    val nStart2 = graph.createInputNode("nStart2")
    val nJoin = graph.createNode("nJoin", new Interleave("nJoin"))
    val nEnd = graph.createOutputNode("nEnd")
    val nOpt = graph.createInputNode("nOpt")
    val nExpr = graph.createNode("nExpr", new XPathExpression("nExpr", "Hello {$user}"))

    graph.addEdge(nStart1, "result", nJoin, "left")
    graph.addEdge(nStart2, "result", nJoin, "right")
    graph.addEdge(nJoin, "result", nEnd, "source")
    graph.addEdge(nOpt, "result", nExpr, "user")
    graph.addEdge(nExpr, "result", nJoin, "left")

    val valid = graph.valid()
    println(valid)
    graph.dump()

    /*
    if (valid) {
      graph.dump()
      graph.makeActors()

      nOpt.write(new StringItem("Fred"))
      nStart1.write(new StringItem("hello"))
      nStart1.write(new StringItem("hello"))
      nStart2.write(new StringItem("goodbye"))
      nStart2.write(new StringItem("goodbye"))

      nOpt.close()
      nStart1.close()
      nStart2.close()
      var running = true
      while (running) {
        Thread.sleep(100)
        running = !graph.finished
      }

      var item = nEnd.read()
      while (item.isDefined) {
        println("OUTPUT: " + item.get)
        item = nEnd.read()
      }
    }
    */
  }

  def testGraph3(): Unit = {
    val left  = graph.createNode("l", new NumberLiteral(3))
    val right = graph.createNode("r", new NumberLiteral(4))
    val add   = graph.createNode("p", new AddExpr(List("+")))
    val add2  = graph.createNode("p2", new AddExpr(List("+")))

    val nfoo = graph.createInputNode("nfoo")
    val nEnd = graph.createOutputNode("nEnd")

    graph.addEdge(left, "result", add, "left")
    graph.addEdge(right, "result", add, "right")
    graph.addEdge(add, "result", add2, "left")
    graph.addEdge(nfoo, "result", add2, "right")
    graph.addEdge(add2, "result", nEnd, "source")

    val valid = graph.valid()
    println(valid)

    if (valid) {
      val runtime = new XProcRuntime(graph)
      runtime.start()

      nfoo.write(new NumberItem(36))
      nfoo.close()

      while (runtime.running) {
        Thread.sleep(100)
      }

      var item = nEnd.read()
      while (item.isDefined) {
        println("OUTPUT: " + item.get)
        item = nEnd.read()
      }
    }
  }
}
