package com.xmlcalabash.runtime

import com.xmlcalabash.core.XProcException
import com.xmlcalabash.items.StringItem
import com.xmlcalabash.messages.ItemMessage

/**
  * Created by ndw on 10/3/16.
  */
class XPathExpression(name: String, expr: String) extends Step {
  private var controller: StepController = _
  private var inputPorts: collection.Set[String] = _
  private var options: collection.Set[String] = _
  private var vars = collection.mutable.HashMap.empty[String, String]

  override def init(controller: StepController,
                    inputPorts: collection.Set[String],
                    outputPorts: collection.Set[String],
                    options: collection.Set[String]): Boolean = {
    this.controller = controller
    this.inputPorts = inputPorts
    this.options = options

    // Fail if the output isn't configured correctly
    outputPorts.size == 1 && outputPorts.contains("result")
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    msg.item match {
      case item: StringItem => vars.put(port, item.get)
      case _ => throw new XProcException("Only strings can be passed to an XPathExpression")
    }
  }

  override def run(): Unit = {
    var result = expr
    for (name <- vars.keySet) {
      val value = vars(name)
      result = result.replaceAll("\\{\\$" + name + "\\}", value)
    }
    controller.send("result", new StringItem(result))
  }
}
