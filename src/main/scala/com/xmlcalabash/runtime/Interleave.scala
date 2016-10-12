package com.xmlcalabash.runtime

import com.jafpl.items.GenericItem
import com.jafpl.runtime.{DefaultStep, StepController}
import com.xmlcalabash.core.XProcException
import com.jafpl.messages.ItemMessage
import net.sf.saxon.s9api.QName

/**
  * Created by ndw on 10/3/16.
  */
class Interleave extends DefaultStep {
  private val leftSource = collection.mutable.ListBuffer.empty[GenericItem]
  private val rightSource = collection.mutable.ListBuffer.empty[GenericItem]

  override def setup(ctrl: StepController,
                     inputs: List[String],
                     outputs: List[String]): Unit = {
    super.setup(ctrl, inputs, outputs)
    if (inputPorts.size == 2 && inputPorts.contains("left") && inputPorts.contains("right")) {
      // nop
    } else {
      throw new XProcException("Misconfigured Interleave")
    }
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    super.receive(port, msg)
    if (port == "left") {
      leftSource.append(msg.item)
    } else {
      rightSource.append(msg.item)
    }

    if (leftSource.nonEmpty && rightSource.nonEmpty) {
      controller.send("result", leftSource.remove(0))
      controller.send("result", rightSource.remove(0))
    }
  }

  override def run(): Unit = {
    while (leftSource.nonEmpty || rightSource.nonEmpty) {
      if (leftSource.nonEmpty) {
        controller.send("result", leftSource.remove(0))
      }
      if (rightSource.nonEmpty) {
        controller.send("result", rightSource.remove(0))
      }
    }
  }
}
