package com.xmlcalabash.drivers

import com.xmlcalabash.core.XProcEngine
import com.xmlcalabash.graph.{Graph, XProcRuntime}
import com.xmlcalabash.items.StringItem
import com.xmlcalabash.runtime.{Identity, Interleave, XPathExpression}
import net.sf.saxon.s9api.Processor

object GraphTest extends App {
  val processor = new Processor(false)
  val engine = new XProcEngine(processor)

  val graph = new Graph(engine)

  testGraph2()

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
}
