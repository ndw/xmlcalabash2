package com.xmlcalabash.model.xml

import java.io.PrintWriter

import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/4/16.
  */
class Output(node: Option[XdmNode], parent: Option[Artifact]) extends InputOrOutput(node, parent) {
  _xmlname = "output"

  override def fixBindingsOnIO(): Unit = {
    if (parent.isDefined && parent.get.isInstanceOf[CompoundStep]) {
      super.fixBindingsOnIO()
    }
  }

  /*
  override def printGraph(pw: PrintWriter): Unit = {
    pw.println("subgraph cluster" + uid.toString + " {")
    pw.println("label = " + xmlname + ";")
    pw.println(dotName())
    for (child <- children) {
      child.printGraph(pw)
    }
    pw.println("}")
  }
  */

}
