package com.xmlcalabash.runtime

import com.xmlcalabash.messages.ItemMessage

/**
  * Created by ndw on 10/8/16.
  */
trait CompoundEnd extends Step {
  def receiveResult(port: String, msg: ItemMessage)

}
