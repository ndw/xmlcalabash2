package com.xmlcalabash.model.xml.util

import net.sf.saxon.s9api.QName

class WithOptionData(val name: QName, val port: String, val select: String, val nsBindings: Map[String,String]) {

}
