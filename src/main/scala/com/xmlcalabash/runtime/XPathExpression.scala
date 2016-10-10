package com.xmlcalabash.runtime

import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.{DefaultStep, StepController}
import com.xmlcalabash.core.XProcException
import com.xmlcalabash.items.StringItem
import net.sf.saxon.s9api.QName

/**
  * Created by ndw on 10/3/16.
  */
class XPathExpression(expr: String, name: String) extends DefaultStep(name) {
  private var vars = collection.mutable.HashMap.empty[String, String]
  private var context: Option[ItemMessage] = None

  override def setup(ctrl: StepController,
                     inputs: List[String],
                     outputs: List[String],
                     opts: List[QName]): Unit = {
    super.setup(ctrl, inputs, outputs, opts)
    if (outputPorts.isEmpty || (outputPorts.size == 1 && outputPorts.contains("result"))) {
      // nop
    } else {
      throw new XProcException("Misconfigured xpath expression step")
    }
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    super.receive(port, msg)
    if (port == "source") {
      if (context.isDefined) {
        throw new XProcException("Sequence on xpath context")
      } else {
        context = Some(msg)
      }
    } else {
      msg.item match {
        case item: StringItem => vars.put(port, item.get)
        case _ => throw new XProcException("Only strings can be passed to an XPathExpression")
      }
    }
  }

  override def run(): Unit = {
    if (outputPorts.nonEmpty) {
      var result = expr
      for (name <- vars.keySet) {
        val value = vars(name)
        result = result.replaceAll("\\{\\$" + name + "\\}", value)
      }
      controller.send("result", new StringItem(result))
    }
  }
}
