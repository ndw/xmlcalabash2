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

  override def receive(port: String, msg: ItemMessage): Unit = {
    if (port.startsWith("{")) {
      val value = msg.item match {
        case xdm: XPathDataModelItem => xdm.value.toString
        case str: StringItem => str.get
        case any: Any =>
          println("Error: Unexpected item")
          ""
      }

      val qname = parseClarkName(port)
      qname match {
        case StepConstants._code => code = Some(value)
        case StepConstants._code_namespace => codeNamespace = Some(value)
        case StepConstants._code_prefix => codePrefix = Some(value)
        case _ =>
          println("Unexpected option: " + qname)
      }
    } else {
      // This is a black hole
    }
  }

  override def run(): Unit = {
    var errCode: Option[QName] = None

    /*
    if (code.isDefined) {
      if (code.get.contains(":")) {
        if (codePrefix.isDefined || codeNamespace.isDefined) {

        }
      } else {
      }
    }
    */

    logger.info("RUNNING error: " + label)
  }

}
