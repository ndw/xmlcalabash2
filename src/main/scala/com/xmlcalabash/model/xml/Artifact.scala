package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcException
import com.xmlcalabash.model.SourceLocation
import com.xmlcalabash.model.xml.util.TreeWriter
import com.xmlcalabash.util.UniqueId
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/1/16.
  */
abstract class Artifact(val context: Option[XdmNode]) {
  val id = UniqueId.nextId
  private var _location: Option[SourceLocation] = None
  private var _excludeInlinePrefixes: Option[List[String]] = None

  if (context.isDefined) {
    _location = Some(new SourceLocation(context.get.getBaseURI, context.get.getLineNumber, context.get.getColumnNumber))
  }

  def location = _location

  // FIXME: only support excludeInlinePrefixes on appropriate elements

  def excludeInlinePrefixes = _excludeInlinePrefixes

  def excludeInlinePrefixes_=(value: Option[String]): Unit = {
    if (value.isDefined) {
      if (value.get.trim == "") {
        _excludeInlinePrefixes = None
      } else {
        _excludeInlinePrefixes = Some(value.get.split("\\s+").toList)
      }

      // FIXME: check for valid prefixes in context
    }
  }

  def staticError(msg: String): Unit = {
    println("STATIC ERROR: " + msg)
    throw new XProcException(msg)
  }

  def dump(tree: TreeWriter): Unit
}
