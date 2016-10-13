package com.xmlcalabash.runtime

import com.jafpl.runtime.DefaultStep
import com.xmlcalabash.core.{XProcConstants, XProcEngine}
import net.sf.saxon.s9api._

import scala.collection.mutable

/**
  * Created by ndw on 10/11/16.
  */
class DefaultXProcStep extends DefaultStep with XProcStep {
  private var _engine: XProcEngine = _
  protected val options = mutable.HashMap.empty[QName, XdmValue]

  override def engine: XProcEngine = _engine

  override def engine_=(engine: XProcEngine): Unit = {
    _engine = engine
  }

  def parseClarkName(name: String): QName = {
    if (name.startsWith("{}")) {
      new QName("", name.substring(2))
    } else {
      val pos = name.indexOf("}")
      val uri = name.substring(1, pos-1)
      val localName = name.substring(pos+1)
      new QName(uri, localName)
    }
  }

  def getUntypedAtomic(proc: Processor, str: String): XdmValue = {
    val itf = new ItemTypeFactory(proc)
    val untypedAtomic = itf.getAtomicType(XProcConstants.xs_untypedAtomic)
    new XdmAtomicValue(str, untypedAtomic)
  }

  def getStringOption(name: QName): Option[String] = {
    val value = options.get(name)
    if (value.isDefined) {
      Some(value.get.toString)
    } else {
      None
    }
  }

  def getQNameOption(name: QName): Option[QName] = {
    val value = getStringOption(name)
    if (value.isDefined) {
      val pos = value.get.indexOf(":")
      if (pos <= 0) {
        Some(new QName("", value.get))
      } else {
        val prefix = value.get.substring(0, pos)
        val localName = value.get.substring(pos+1)
        // FIXME: need namespace bindings
        Some(new QName("", "ERROR"))
      }
    } else {
      None
    }
  }
}
