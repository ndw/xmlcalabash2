package com.xmlcalabash.core

import com.xmlcalabash.model.xml.decl.XProc11Steps
import com.xmlcalabash.runtime.{Identity, XProcStep}
import net.sf.saxon.s9api.{ItemTypeFactory, Processor, QName, XdmAtomicValue, XdmNode, XdmValue}
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * Created by ndw on 10/1/16.
  */
class XProcEngine(val processor: Processor) {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  val stdLibrary = new XProc11Steps()
  val itemTypeFactory = new ItemTypeFactory(processor)

  def implementation(stepType: QName): XProcStep = {
    val map = mutable.HashMap(
      XProcConstants.p_identity -> "com.xmlcalabash.runtime.Identity",
      XProcConstants.p_xslt -> "com.xmlcalabash.runtime.Xslt"
    )

    val className = map.get(stepType)
    val classObj = if (className.isDefined) {
      Class.forName(className.get).newInstance()
    } else {
      logger.warn("No implmentation for: " + stepType)
      new Identity()
    }

    val obj = classObj.asInstanceOf[XProcStep]
    obj.engine = this
    obj.label = stepType.getLocalName
    obj
  }

  def getUntypedAtomic(str: String): XdmValue = {
    val untypedAtomic = itemTypeFactory.getAtomicType(XProcConstants.xs_untypedAtomic)
    new XdmAtomicValue(str, untypedAtomic)
  }


  def staticError(node: Option[XdmNode], msg: String): Unit = {
    println("Static error: " + msg)
  }

  def dynamicError(node: Option[XdmNode], msg: String): Unit = {
    println("Dynamic error:" + msg)
  }

  def dynamicError(throwable: Throwable): Unit = {
    println("Dynamic error:" + throwable.getCause.getMessage)
    throw throwable
  }
}
