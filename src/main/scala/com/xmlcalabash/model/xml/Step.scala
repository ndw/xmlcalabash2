package com.xmlcalabash.model.xml

import java.io.PrintWriter

import com.xmlcalabash.core.XProcConstants
import net.sf.saxon.s9api.{QName, XdmNode}

/**
  * Created by ndw on 10/5/16.
  */
abstract class Step(node: Option[XdmNode], parent: Option[Artifact]) extends Artifact(node, parent) {
  private val falseAttr = new Attribute(new QName("", "irrelevant"), "false")
  protected var _atomic = false

  def atomic = _atomic

  def primaryInputPort: Option[InputOrOutput] = {
    var primary: Option[Input] = None

    for (child <- _children) {
      child match {
        case i: Input =>
          if (i.property(XProcConstants._primary).getOrElse(falseAttr).value == "true") {
            primary = Some(i)
          }
        case _ => Unit
      }
    }
    primary
  }

  def primaryOutputPort: Option[Output] = {
    var primary: Option[Output] = None

    for (child <- _children) {
      child match {
        case o: Output =>
          if (o.property(XProcConstants._primary).getOrElse(falseAttr).value == "true") {
            primary = Some(o)
          }
        case _ => Unit
      }
    }
    primary
  }
}
