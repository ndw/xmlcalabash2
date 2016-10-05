package com.xmlcalabash.core

import com.xmlcalabash.model.xml.decl.XProc10Steps
import net.sf.saxon.s9api.{Processor, QName, XdmNode}

/**
  * Created by ndw on 10/1/16.
  */
class XProcEngine(val processor: Processor) {
  val stdLibrary = new XProc10Steps()

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
