package com.xmlcalabash.model.xml

import com.xmlcalabash.model.xml.bindings.Binding
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/1/16.
  */
abstract class IODeclaration(context: Option[XdmNode], val port: Option[String]) extends Artifact(context) {
  private var _bindings: Option[List[Binding]] = None

  def bindings = _bindings

  def addBinding(binding: Binding) {
    if (_bindings.isDefined) {
      _bindings = Some(_bindings.get ::: List(binding))
    } else {
      _bindings = Some(List(binding))
    }
  }
}
