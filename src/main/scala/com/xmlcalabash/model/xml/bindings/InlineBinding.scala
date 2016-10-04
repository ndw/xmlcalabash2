package com.xmlcalabash.model.xml.bindings

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.util.{NodeUtils, TreeWriter}
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/1/16.
  */
class InlineBinding(override val context: Option[XdmNode], val document: XdmNode) extends Binding(context: Option[XdmNode]) {
  var _expandText: Option[Boolean] = None
  var _contentType: Option[String] = None
  var _encoding: Option[String] = None

  def expandText = _expandText
  def contentType = _contentType
  def encoding = _encoding

  def expandText_=(value: Option[Boolean]): Unit = {
    _expandText = value
  }

  def contentType_=(value: Option[String]): Unit = {
    _contentType = value
  }

  def encoding_=(value: Option[String]): Unit = {
    _encoding = value
  }

  def dump(tree: TreeWriter): Unit = {
    tree.addStartElement(XProcConstants.px("inline"))
    tree.addSubtree(document)
    tree.addEndElement()
  }
}
