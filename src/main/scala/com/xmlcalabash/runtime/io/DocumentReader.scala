package com.xmlcalabash.runtime.io

import java.io.File

import com.jafpl.runtime.DefaultStep
import com.xmlcalabash.core.XProcEngine
import com.xmlcalabash.items.{StringItem, XPathDataModelItem}

/**
  * Created by ndw on 10/10/16.
  */
class DocumentReader(engine: XProcEngine, uri: String) extends DefaultStep {
  val document = uri
  label = "_doc_reader"

  override def run(): Unit = {
    super.run()

    // Assume it's XML
    val builder = engine.processor.newDocumentBuilder()
    val doc = builder.build(new File(uri))
    controller.send("result", new XPathDataModelItem(doc))
  }
}
