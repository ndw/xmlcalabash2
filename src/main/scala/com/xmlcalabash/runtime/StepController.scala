package com.xmlcalabash.runtime

import com.xmlcalabash.items.GenericItem

/**
  * Created by ndw on 10/3/16.
  */
trait StepController {
  def send(port: String, item: GenericItem)
}
