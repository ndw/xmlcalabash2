package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.params.EmptyLoaderParams

class Empty(override val config: XMLCalabashConfig) extends DataSource(config) {

  override protected[model] def normalizeToPipes(): Unit = {
    val params = new EmptyLoaderParams(staticContext)
    normalizeDataSourceToPipes(XProcConstants.cx_empty_loader, params)
  }

  override def toString: String = {
    if (tumble_id.startsWith("!syn")) {
      s"p:empty"
    } else {
      s"p:empty $tumble_id"
    }
  }
}
