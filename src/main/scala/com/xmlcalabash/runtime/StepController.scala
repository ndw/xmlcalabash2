package com.xmlcalabash.runtime

import com.xmlcalabash.graph.Node
import com.xmlcalabash.items.GenericItem

/**
  * Created by ndw on 10/3/16.
  */
trait StepController {
  def send(port: String, item: GenericItem)
  def close(port: String)
  def tell(node: Node, msg: Any)
  def stop()
}
