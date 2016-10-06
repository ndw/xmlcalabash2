package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.graph.{Graph, Node}
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable

/**
  * Created by ndw on 10/4/16.
  */
class DeclOption(node: Option[XdmNode], parent: Option[Artifact]) extends Artifact(node, parent) {
  override def fixBindingsOnIO(): Unit = {
    if (bindings.isEmpty) {
      if (defaultReadablePort.isDefined) {
        val pipe = new Pipe(None, Some(this))
        pipe._drp = defaultReadablePort
        _children += pipe
      }
    }
    for (child <- _children) { child.fixBindingsOnIO() }
  }

  override def buildNodes(graph: Graph, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    val name = property(XProcConstants._name).get.value
    val optName = new QName("", name) // FIXME: pass real qnames
    val node = graph.createInputOption(optName)
    nodeMap.put(this, node)
  }

}
