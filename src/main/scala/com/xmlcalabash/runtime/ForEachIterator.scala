package com.xmlcalabash.runtime

import com.xmlcalabash.graph.Node
import com.xmlcalabash.items.GenericItem
import com.xmlcalabash.messages.{ItemMessage, ResetMessage}
import net.sf.saxon.s9api.QName
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/3/16.
  */
class ForEachIterator(name: String, subpipeline: List[Node]) extends DefaultStart(name, subpipeline) {
  private val items = ListBuffer.empty[GenericItem]
  private var ran = false

  override def reset(): Unit = {
    semaphore.synchronized {
      ran = false
      items.clear()
    }
  }

  override def run(): Unit = {
    semaphore.synchronized {
      logger.info("RUN FOR EACH: " + ready + ": " + items.size)
      ran = true
      if (ready) {
        if (items.nonEmpty) {
          ready = false
          restartPipeline()
          logger.info("SEND ON FOR EACH")
          controller.send("current", items.head)
          controller.close("current")
          items.remove(0)
        }
      }
    }
  }

  override def readyToRestart(): Unit = {
    semaphore.synchronized {
      logger.info("READY TO RESTART ON FOR EACH: " + items.size + ": " + ran)
      super.readyToRestart()
      if (items.nonEmpty) {
        ready = false
        restartPipeline()
        logger.info("SEND ON FOR EACH")
        controller.send("current", items.head)
        controller.close("current")
        items.remove(0)
      }
    }
  }

  private def restartPipeline(): Unit = {
    logger.info("RESTART FOR EACH: " + items.size)
    needsRestart = false
    for (node <- subpipeline) {
      controller.tell(node, new ResetMessage())
    }
  }

  override def finished: Boolean = {
    semaphore.synchronized {
      logger.info("FINISHED FOR EACH: " + ran + ": "+ items.size)
      ran && items.isEmpty
    }
  }

  override def completed: Boolean = {
    semaphore.synchronized {
      logger.info("COMPLETED FOR EACH: " + ran + ": "+ items.size)
      if (ran && items.isEmpty) {
        controller.stop()
        true
      } else {
        false
      }
    }
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    semaphore.synchronized {
      logger.info("RECEIVED ON FOR EACH: " + ready)
      logger.debug("{} receive on {}: {}", name, port, msg)
      if (ready) {
        ready = false
        if (needsRestart) {
          restartPipeline()
        }

        var item = msg.item
        if (items.nonEmpty) {
          items += msg.item
          item = items.head
          items.remove(0)
        }
        logger.info("SEND ON FOR EACH")
        controller.send("current", item)
        controller.close("current")
      } else {
        items += msg.item
      }
    }
  }
}
