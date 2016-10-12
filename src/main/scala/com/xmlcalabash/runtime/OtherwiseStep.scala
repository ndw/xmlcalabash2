package com.xmlcalabash.runtime

import com.jafpl.items.GenericItem
import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.DefaultCompoundStep

import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/10/16.
  */
class OtherwiseStep extends DefaultCompoundStep {
  val docs = ListBuffer.empty[GenericItem]
  label = "p_otherwise"

  override def run(): Unit = {
    logger.info("RUN   When {}", docs.size)
    if (docs.nonEmpty) {
      val item = docs.head
      docs.remove(0)
      controller.send("current", item)
      controller.close("current")
    }
  }

  override def runAgain: Boolean = {
    logger.info("AGAIN When {}", docs.size)
    docs.nonEmpty
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    logger.info("RECV  When {}", docs.size)
    docs += msg.item
  }
}
