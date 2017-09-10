package com.xmlcalabash.runtime

import net.sf.saxon.s9api.XdmItem

trait XProcDataConsumer {
  def receive(port: String, item: Any, metadata: XProcMetadata)
}
