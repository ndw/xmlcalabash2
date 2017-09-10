package com.xmlcalabash.steps

import com.xmlcalabash.runtime.XmlPortSpecification

class Sink extends DefaultXmlStep {
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.NONE
}
