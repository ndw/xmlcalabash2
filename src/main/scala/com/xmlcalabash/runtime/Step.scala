package com.xmlcalabash.runtime

import com.xmlcalabash.messages.ItemMessage

/**
  * Created by ndw on 10/3/16.
  */
trait Step {
  def init(controller: StepController,
           inputPorts: collection.Set[String],
           outputPorts: collection.Set[String],
           options: collection.Set[String]): Boolean
  def receive(port: String, msg: ItemMessage)
  def run()
}
