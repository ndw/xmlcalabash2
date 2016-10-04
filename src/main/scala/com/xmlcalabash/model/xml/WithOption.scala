package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.bindings.Binding
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.{QName, XdmNode}

/**
  * Created by ndw on 10/1/16.
  */

// I'd have just called this Option except that that interferes with Scala Option.
class WithOption(context: Option[XdmNode], val name: QName, val select: String) extends Artifact(context) {
  private var _as: Option[String] = None
  private var _binding: Option[Binding] = None
  private var _namespaces: Option[List[Namespaces]] = None

  def as = _as
  def binding = _binding
  def namespaces = _namespaces

  def as_=(value: String): Unit = {
    _as = Some(value)
  }

  def binding_=(binding: Binding): Unit = {
    _binding = Some(binding)
  }

  def addNamespaces(ns: Namespaces): Unit = {
    if (_namespaces.isDefined) {
      _namespaces = Some(_namespaces.get ::: List(ns))
    } else {
      _namespaces = Some(List(ns))
    }
  }

  override def dump(tree: TreeWriter): Unit = {
    tree.addStartElement(XProcConstants.px("option-declaration"))
    tree.addAttribute(XProcConstants._name, name.toString)
    tree.addAttribute(XProcConstants._select, select)
    if (as.isDefined) {
      tree.addAttribute(XProcConstants._as, as.get)
    }
    if (binding.isDefined) {
      binding.get.dump(tree)
    }
    if (namespaces.isDefined) {
      namespaces.get.foreach { _.dump(tree) }
    }
    tree.addEndElement()
  }
}
