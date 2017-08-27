package com.xmlcalabash.model.xml

import net.sf.saxon.s9api.QName

class AtomicStep(override val parent: Option[Artifact], val stepType: QName) extends Step(parent) {
  private var _name: Option[String] = None

  override def validate(): Boolean = {
    _name = properties.get(XProcConstants._name)
    var valid = true

    for (key <- List(XProcConstants._name)) {
      if (properties.contains(key)) {
        properties.remove(key)
      }
    }

    for (key <- properties.keySet) {
      if (key.getNamespaceURI == "") {
        throw new XmlPipelineException("badopt", s"Unexpected attribute: ${key.getLocalName}")
      }
    }

    val okChildren = List(classOf[Input], classOf[WithOption], classOf[Log])
    for (child <- children) {
      if (!okChildren.contains(child.getClass)) {
        throw new XmlPipelineException("badelem", s"Unexpected element: $child")
      }
      valid = valid && child.validate()
    }

    valid
  }

  override def addDeclaredBindings(): Unit = {
    super.addDeclaredBindings()
    stepType match {
      case XProcConstants.p_identity =>
        if (input("source").isEmpty) {
          val in = new Input(this, "source", true, true)
          addChild(in)
        }
        if (output("result").isEmpty) {
          val out = new Output(this, "result", true, true)
          addChild(out)
        }
      case _ =>
        throw new XmlPipelineException("badstep", s"Unknown step type: $stepType")
    }

  }

}
