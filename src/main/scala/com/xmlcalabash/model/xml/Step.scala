package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.s9api.XdmNode

class Step(override val config: XMLCalabashConfig) extends Artifact(config) with NamedArtifact {
  protected[xml] var _name = Option.empty[String]
  override def stepName: String = _name.getOrElse(tumble_id)
  protected[model] def stepName_=(name: String): Unit = {
    _name = Some(name)
  }

  override def parse(node: XdmNode): Unit = {
    super.parse(node)
    _name = attr(XProcConstants._name)
  }

  override protected[model] def validateStructure(): Unit = {
    for (child <- allChildren) {
      child.validateStructure()
    }
  }

  protected[model] def primaryInput: Option[Port] = {
    for (child <- allChildren) {
      child match {
        case input: DeclareInput =>
          if (input.primary) {
            return Some(input)
          }
        case winput: WithInput =>
          if (winput.primary) {
            return Some(winput)
          }
        case _ => Unit
      }
    }
    None
  }

  protected[model] def primaryOutput: Option[Port] = {
    for (child <- allChildren) {
      child match {
        case output: DeclareOutput =>
          if (output.primary) {
            return Some(output)
          }
        case woutput: WithOutput =>
          if (woutput.primary) {
            return Some(woutput)
          }
        case _ => Unit
      }
    }
    None
  }
}
