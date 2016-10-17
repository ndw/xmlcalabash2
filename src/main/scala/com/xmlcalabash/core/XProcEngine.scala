package com.xmlcalabash.core

import com.jafpl.util.SourceLocation
import com.xmlcalabash.model.xml.decl.XProc11Steps
import com.xmlcalabash.runtime.{Identity, XProcStep}
import net.sf.saxon.s9api.{ItemTypeFactory, Processor, QName, XdmAtomicValue, XdmNode, XdmValue}
import org.slf4j.LoggerFactory

/**
  * Created by ndw on 10/1/16.
  */
class XProcEngine(val processor: Processor) {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  val cwd = System.getProperty("user.dir") + "/"
  val stdLibrary = new XProc11Steps()
  val itemTypeFactory = new ItemTypeFactory(processor)

  def implementation(stepType: QName): XProcStep = {
    val map = Map(
      XProcConstants.p_identity -> "com.xmlcalabash.runtime.Identity",
      XProcConstants.p_xslt -> "com.xmlcalabash.runtime.Xslt",
      XProcConstants.p_error -> "com.xmlcalabash.runtime.Error"
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

  def stepError(code: QName, location: Option[SourceLocation], msg: String): Unit = {
    stepError(Some(code), location, msg)
  }

  def stepError(location: Option[SourceLocation], msg: String): Unit = {
    stepError(None, location, msg)
  }

  def stepError(code: Option[QName], location: Option[SourceLocation], msg: String): Unit = {
    var formatted = ""
    if (location.isDefined) {
      var path = location.get.baseURI.toASCIIString
      if (path.startsWith("file:" + cwd) || path.startsWith("file://" + cwd)) {
        val pos = path.indexOf(cwd)
        path = path.substring(pos + cwd.length)
      }
      formatted += path + ":"
      if (location.get.lineNumber > 0) {
        formatted += location.get.lineNumber + ":" + location.get.columnNumber + ":"
      }
    }
    if (code.isDefined) {
      formatted += code.toString + ":"
    }
    throw new XProcException(code, formatted+msg)
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
