package com.xmlcalabash.config

import net.sf.saxon.s9api.{QName, XdmValue}

object XProcTypes {
  type Parameters = Map[QName,XdmValue]
}
