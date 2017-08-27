package com.xmlcalabash.runtime

import com.jafpl.steps.DataProvider

class PrintingConsumer extends DataProvider {
  override def send(item: Any): Unit = {
    println(item.toString)
  }

  override def close(): Unit = Unit
}
