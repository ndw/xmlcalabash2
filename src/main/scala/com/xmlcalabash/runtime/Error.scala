package com.xmlcalabash.runtime

import com.jafpl.messages.ItemMessage
import com.xmlcalabash.core.StepConstants
import com.xmlcalabash.items.{StringItem, XPathDataModelItem}
import net.sf.saxon.s9api.QName

/**
  * Created by ndw on 10/3/16.
  */
class Error extends DefaultXProcStep {
  var code: Option[String] = None
  var codePrefix: Option[String] = None
  var codeNamespace: Option[String] = None
  var nsbindings = Map.empty[String,String]

  override def receive(port: String, msg: ItemMessage): Unit = {
    if (port.startsWith("{")) {
      var value = ""
      var bindings = Map.empty[String,String]

      msg.item match {
        case xdm: XPathDataModelItem =>
          bindings = xdm.inscopeNamespaces
          value = xdm.value.toString
        case str: StringItem =>
          value = str.get
        case any: Any =>
          println("Error: Unexpected item type: " + any)
      }

      val qname = parseClarkName(port)
      qname match {
        case StepConstants._code =>
          code = Some(value)
          nsbindings = bindings
        case StepConstants._code_namespace =>
          codeNamespace = Some(value)
        case StepConstants._code_prefix =>
          codePrefix = Some(value)
        case _ =>
          println("Unexpected option: " + qname)
      }
    } else {
      // This is a black hole
    }
  }

  override def run(): Unit = {
    var errCode: Option[QName] = None

    if (code.isDefined) {
      if (code.get.contains(":")) {
        if (codePrefix.isDefined || codeNamespace.isDefined) {
          engine.stepError(StepConstants.xd(34), location, "If code contains a colon, you may not specify code-prefix or code-namespace")
        }
        val pos = code.get.indexOf(":")
        val pfx = code.get.substring(0, pos)
        val lcl = code.get.substring(pos+1)
        if (nsbindings.contains(pfx)) {
          errCode = Some(new QName(pfx, nsbindings(pfx), lcl))
        } else {
          println("Error: no binding for pfx: " + pfx + ": " + code.get)
        }
      } else {
        errCode = Some(new QName("", code.get))
      }
    }

    logger.info("RUNNING error: " + label + ": " + errCode)
  }

}
