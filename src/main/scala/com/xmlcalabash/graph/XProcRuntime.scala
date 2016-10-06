package com.xmlcalabash.graph

import com.xmlcalabash.core.XProcException
import com.xmlcalabash.items.GenericItem
import net.sf.saxon.s9api.QName
import org.slf4j.LoggerFactory

/**
  * Created by ndw on 10/6/16.
  */
class XProcRuntime(val graph: Graph) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val engine = graph.engine
  private var started = false

  if (!graph.valid) {
    engine.staticError(None, "Graph is invalid")
    throw new XProcException("Invalid graph")
  }

  def inputs(): List[InputNode] = {
    if (!started) {
      throw new XProcException("You must start the pipeline first!")
    }

    graph.inputs()
  }

  def options(): List[InputOption] = {
    if (!started) {
      throw new XProcException("You must start the pipeline first!")
    }

    graph.options()
  }

  def outputs(): List[OutputNode] = {
    if (!started) {
      throw new XProcException("You must start the pipeline first!")
    }

    graph.outputs()
  }

  def read(port: String): Option[GenericItem] = {
    for (node <- outputs()) {
      if (node.name.isDefined && node.name.get == port) {
        return node.read()
      }
    }

    logger.info("Pipeline has no output port: " + port)
    None
  }

  def set(optName: QName, item: GenericItem): Unit = {
    if (!started) {
      throw new XProcException("You must start the pipeline first!")
    }

    for (opt <- options()) {
      if (opt.optName == optName) {
        opt.set(item)
        return
      }
    }

    logger.info("Pipeline has no option named: " + optName)
  }

  def write(port: String, item: GenericItem): Unit = {
    if (!started) {
      throw new XProcException("You must start the pipeline first!")
    }

    for (node <- inputs()) {
      if (node.name.isDefined && node.name.get == port) {
        node.write(item)
        return
      }
    }

    logger.info("Pipeline has no input port: " + port)
  }

  def close(port: String): Unit = {
    if (!started) {
      throw new XProcException("You must start the pipeline first!")
    }

    for (node <- inputs()) {
      if (node.name.isDefined && node.name.get == port) {
        node.close()
        return
      }
    }

    logger.info("Pipeline has no input port: " + port)
  }

  def start(): Unit = {
    graph.makeActors()
    started = true
  }

  def running: Boolean = !graph.finished

  def kill(): Unit = {
    graph.system.terminate()
  }



}
