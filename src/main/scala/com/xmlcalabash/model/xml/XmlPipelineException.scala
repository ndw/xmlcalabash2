package com.xmlcalabash.model.xml

class XmlPipelineException(val code: String, val message: String) extends Throwable {
  override def toString: String = {
    s"Exception('$code','$message')"
  }
}
