package com.xmlcalabash.model.xml.bindings

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.util.TreeWriter
import com.xmlcalabash.model.xml.{NameDecl, XPathContext}
import net.sf.saxon.s9api.QName

/**
  * Created by ndw on 10/7/16.
  */
class NamePipe(name: QName, parent: XPathContext) extends Binding(None, Some(parent)) {
  _xmlname = "name-pipe"
  var _decl: Option[NameDecl] = None

  def decl = _decl
  def decl_=(decl: NameDecl): Unit = {
    _decl = Some(decl)
  }

  override def dumpAdditionalAttributes(tree: TreeWriter): Unit = {
    if (_decl.isDefined) {
      tree.addAttribute(XProcConstants.px("decl"), _decl.get.toString)
    }
  }
}
