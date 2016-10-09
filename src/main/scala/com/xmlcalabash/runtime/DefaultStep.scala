package com.xmlcalabash.runtime

import com.xmlcalabash.messages.ItemMessage
import net.sf.saxon.s9api.QName
import org.slf4j.LoggerFactory

/**
  * Created by ndw on 10/7/16.
  */
abstract class DefaultStep(name: String) extends Step  {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected var controller: StepController = _
  protected var inputPorts = List.empty[String]
  protected var outputPorts = List.empty[String]
  protected var options = List.empty[QName]

  override def setup(ctrl: StepController,
                     inputs: List[String],
                     outputs: List[String],
                     opts: List[QName]): Unit = {
    logger.debug("{} setup", name)
    controller = ctrl
    inputPorts = inputs
    outputPorts = outputs
    options = opts
  }

  override def reset(): Unit = {
    logger.debug("{} reset", this)
  }

  override def run(): Unit = {
    logger.debug("{} run", this)
  }

  override def teardown() = {
    logger.debug("{} teardown", this)
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    logger.debug("{} receive on {}: {}", name, port, msg)
  }
}
