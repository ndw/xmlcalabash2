package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.bindings.Binding
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.{QName, XdmNode}

/**
  * Created by ndw on 10/1/16.
  */
class Namespaces(context: Option[XdmNode], val name: QName, val select: String) extends Artifact(context) {
  private var _binding: Option[Binding] = None
  private var _element: Option[String] = None
  private var _exceptPrefixes: Option[List[String]] = None

  def binding = _binding
  def element = _element
  def exceptPrefixes = _exceptPrefixes

  def binding_=(binding: Binding): Unit = {
    _binding = Some(binding)
  }

  def element_=(xpathExpression: String): Unit = {
    _element = Some(xpathExpression)
  }

  def exceptPrefixes_=(value: String): Unit = {
    if (value.trim == "") {
      _exceptPrefixes = None
    } else {
      _exceptPrefixes = Some(value.split("\\s+").toList)
    }

    // FIXME: check for valid prefixes in context
  }

  override def dump(tree: TreeWriter): Unit = {
    tree.addStartElement(XProcConstants.px("namespaces"))
    if (element.isDefined) {
      tree.addAttribute(new QName("", "element"), element.get)
    }
    if (exceptPrefixes.isDefined) {
      tree.addAttribute(new QName("", "except-prefixes"), exceptPrefixes.get.mkString(" "))
    }
    if (binding.isDefined) {
      binding.get.dump(tree)
    }
    tree.addEndElement()
  }
}
