package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable

/**
  * Created by ndw on 10/5/16.
  */
class InputOrOutput(node: Option[XdmNode], parent: Option[XMLArtifact]) extends XMLArtifact(node, parent) {
  def primary: Boolean = {
    val p = property(XProcConstants._primary)
    p.isDefined && p.get.value == "true"
  }

  override def fixUnwrappedInlines(): Unit = {
    if (_children.size == 1 && _children.head.isInstanceOf[XMLLiteral]) {
      val inline = new Inline(None, Some(this))
      inline.xmlname = "inline"
      inline.addChild(_children.head)
      _children(0) = inline
    }
  }

  override def fixBindingsOnIO(): Unit = {
    if (_children.isEmpty) {
      if (property(XProcConstants._step).isDefined) {
        val pipe = new Pipe(None, Some(this))
        pipe.addProperty(XProcConstants._step, property(XProcConstants._step).get.value)
        pipe._drp = defaultReadablePort
        _children += pipe
        removeProperty(XProcConstants._step)
      } else {
        if (defaultReadablePort.isDefined) {
          val pipe = new Pipe(None, Some(this))
          pipe._drp = defaultReadablePort
          _children += pipe
        }
      }
    }
    for (child <- _children) { child.fixBindingsOnIO() }
  }
}
