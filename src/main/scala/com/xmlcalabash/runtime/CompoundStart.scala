package com.xmlcalabash.runtime

import com.xmlcalabash.graph.Node
import com.xmlcalabash.messages.ItemMessage

/**
  * Created by ndw on 10/8/16.
  */
trait CompoundStart extends Step {
  def compoundEnd: CompoundEnd
  def subpipeline: List[Node]
  def readyToRestart()
  def finished: Boolean
  def completed: Boolean
  def receiveResult(port: String, msg: ItemMessage)
}
