package com.xmlcalabash.runtime

import com.xmlcalabash.messages.ItemMessage
import net.sf.saxon.s9api.QName

/**
  * Created by ndw on 10/3/16.
  */
trait Step {
  def setup(controller: StepController,
            inputPorts: List[String],
            outputPorts: List[String],
            options: List[QName])
  def reset()
  def run()
  def teardown()
  def receive(port: String, msg: ItemMessage)
}
