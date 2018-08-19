package com.xmlcalabash.util

import java.util

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.parsers.SequenceBuilder
import com.xmlcalabash.runtime.ExpressionContext
import jdk.nashorn.api.scripting.ScriptObjectMirror
import net.sf.saxon.s9api._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object TypeUtils {

  def castAsXml(value: Any): XdmValue = {
    if (value == null) {
      return XdmEmptySequence.getInstance()
    }
    value match {
      case value: XdmValue => value
      case str: String => new XdmAtomicValue(str)
      case int: Integer => new XdmAtomicValue(int)
      case bool: Boolean => new XdmAtomicValue(bool)
      case doub: Double => new XdmAtomicValue(doub)
      case som: ScriptObjectMirror =>
        if (som.isArray) {
          // Is there a faster way to do this?
          val items = ListBuffer.empty[XdmValue]
          for (key <- som.keySet.asScala) {
            val obj = castAsXml(som.get(key))
            items += obj
          }
          var arr = new XdmArray(items.toArray)
          arr
        } else {
          var map = new XdmMap()
          for (key <- som.keySet.asScala) {
            val obj = castAsXml(som.get(key))
            map = map.put(new XdmAtomicValue(key), obj)
          }
          map
        }
      case _ => throw XProcException.xiCastXML(value, None)
    }
  }

  def castAsJava(value: Any): Any = {
    if (value == null) {
      return value
    }
    value match {
      case node: XdmNode =>
        throw XProcException.xiNodesNotAllowed(node)
      case atomic: XdmAtomicValue =>
        atomic.getValue
      case xarr: XdmArray =>
        val list = ListBuffer.empty[Any]
        var idx = 0
        for (idx <- 0  until xarr.arrayLength()) {
          val value = xarr.get(idx)
          list += castAsJava(value)
        }
        list.toArray
      case xmap: XdmMap =>
        val map = xmap.asMap()
        val jmap = mutable.HashMap.empty[Any,Any]
        for (key <- map.asScala.keySet) {
          val value = map.asScala(key)
          jmap.put(castAsJava(key), castAsJava(value))
        }
        jmap.toMap.asJava
      case _ =>
        value
    }
  }

  def mediaType(value: Any): MediaType = {
    value match {
      case v: XdmMap => vnd("map")
      case v: Boolean => vnd("boolean")
      case _ => throw XProcException.xiMediaType(value, None)
    }
  }

  private def vnd(t: String): MediaType = {
    MediaType.parse(s"application/vnd.xmlcalabash.$t+xml")
  }
}

class TypeUtils(val config: XMLCalabash) {
  val typeFactory = new ItemTypeFactory(config.processor)

  def castAtomicAs(value: XdmAtomicValue, xsdtype: Option[QName], context: ExpressionContext): XdmAtomicValue = {
    if (xsdtype.isEmpty) {
      return value
    }

    if ((xsdtype.get == XProcConstants.xs_untypedAtomic) || (xsdtype.get == XProcConstants.xs_string)) {
      return value
    }

    if (xsdtype.get == XProcConstants.xs_QName) {
      return new XdmAtomicValue(ValueParser.parseQName(value.getStringValue, context.nsBindings, None))
    }

    val itype = typeFactory.getAtomicType(xsdtype.get)
    new XdmAtomicValue(value.getStringValue, itype)
  }

  // This was added experimentally to handle lists in literal values for include-filter and exclude-filter.
  // It was subsequently decided that literal values shouldn't be lists, so this is no longer being used.
  // I'm leaving it around for the time being (19 Aug 2018) in case it turns out to be useful somewhere
  // else.
  def castSequenceAs(value: XdmAtomicValue, xsdtype: Option[QName], occurrence: String, context: ExpressionContext): XdmValue = {
    // Today, we only need to handle a sequence of strings
    if (xsdtype.isEmpty || xsdtype.get != XProcConstants.xs_string) {
      throw new IllegalArgumentException("Only lists of strings are supported")
    }

    val builder = new SequenceBuilder()
    val list = builder.parse(value.getStringValue)
    val alist = new util.ArrayList[XdmAtomicValue]

    val itype = typeFactory.getAtomicType(XProcConstants.xs_string)
    for (item <- list) {
      if (item.as != XProcConstants.xs_string) {
        throw new IllegalArgumentException("Only lists of strings are supported")
      }
      alist.add(new XdmAtomicValue(item.item, itype))
    }

    new XdmValue(alist)
  }
}
