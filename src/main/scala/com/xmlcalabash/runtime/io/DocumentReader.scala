package com.xmlcalabash.runtime.io

import com.jafpl.runtime.DefaultStep
import com.xmlcalabash.core.XProcEngine
import com.xmlcalabash.items.StringItem

/**
  * Created by ndw on 10/10/16.
  */
class DocumentReader(engine: XProcEngine, uri: String) extends DefaultStep {
  val document = uri
  label = "_doc_reader"

  override def run(): Unit = {
    super.run()
    controller.send("result", new StringItem(document))
  }
}
