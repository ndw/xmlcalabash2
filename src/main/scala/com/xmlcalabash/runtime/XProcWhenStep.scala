package com.xmlcalabash.runtime

import com.jafpl.items.GenericItem
import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.{DefaultCompoundStep, WhenStep}
import com.xmlcalabash.items.XPathDataModelItem
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmValue}

import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/10/16.
  */
class XProcWhenStep(testExpr: String) extends DefaultCompoundStep with WhenStep {
  private val vars = collection.mutable.HashMap.empty[QName, XdmValue]
  private val context: Option[GenericItem] = None
  private val docs = ListBuffer.empty[GenericItem]

  label = "p_when"

  override def test(msg: GenericItem): Boolean = {
    msg match {
      case item: XPathDataModelItem =>
        val ok = item.value.itemAt(0).asInstanceOf[XdmAtomicValue].getBooleanValue
        ok
      case _ =>
        logger.warn("Test expression didn't produce XDM value")
        false
    }
  }

  override def run(): Unit = {
    logger.info("RUN   Otherwise {}", docs.size)
    if (docs.nonEmpty) {
      val item = docs.head
      docs.remove(0)
      controller.send("current", item)
      controller.close("current")
    }
  }

  override def runAgain: Boolean = {
    logger.info("AGAIN Otherwise {}", docs.size)
    docs.nonEmpty
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    logger.info("RECV  Otherwise {}", docs.size)
    docs += msg.item
  }
}
