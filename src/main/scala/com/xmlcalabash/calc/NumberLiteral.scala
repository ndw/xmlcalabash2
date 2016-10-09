package com.xmlcalabash.calc

import com.xmlcalabash.core.XProcException
import com.xmlcalabash.items.NumberItem
import com.xmlcalabash.messages.ItemMessage
import com.xmlcalabash.runtime.{Step, StepController}
import net.sf.saxon.s9api.QName

/**
  * Created by ndw on 10/7/16.
  */
class NumberLiteral(val number: Int) extends Step {
  var controller: StepController = _

  override def setup(controller: StepController, inputPorts: List[String], outputPorts: List[String], options: List[QName]): Unit = {
    this.controller = controller
  }

  override def reset(): Unit = {
    // nop
  }

  override def run(): Unit = {
    val item = new NumberItem(number)
    controller.send("result", item)
  }

  override def teardown() = {
    // nop
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    // nop
  }
}
