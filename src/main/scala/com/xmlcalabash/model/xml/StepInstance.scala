package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.{QName, XdmNode}

/**
  * Created by ndw on 10/1/16.
  */
class StepInstance(context: Option[XdmNode], val stepType: QName) extends Step(context) {
  private var _inputs: Option[List[Input]] = None
  private var _options: Option[List[WithOption]] = None
  private var _defaultReadableStep: Option[Step] = _

  def inputs = _inputs
  def options = _options

  def addInput(input: Input): Unit = {
    if (_inputs.isDefined) {
      for (curInput <- _inputs.get) {
        if (curInput.port.get == input.port.get) {
          staticError("Cannot have two inputs with the same port name: " + input.port.get)
        }
      }
      _inputs = Some(_inputs.get ::: List(input))
    } else {
      _inputs = Some(List(input))
    }
  }

  def addOption(option: WithOption): Unit = {
    if (_options.isDefined) {
      for (curOption <- _options.get) {
        if (curOption.name.equals(option.name)) {
          staticError("Cannot declare two options with the same name: " + option.name)
        }
      }
      _options = Some(_options.get ::: List(option))
    } else {
      _options = Some(List(option))
    }
  }

  private[xml] def defaultReadableStep = _defaultReadableStep
  private[xml] def defaultReadableStep_=(value: Option[Step]) {
    _defaultReadableStep = value
  }

  override def dump(tree: TreeWriter): Unit = {
    tree.addStartElement(XProcConstants.px("step"))
    tree.addAttribute(XProcConstants._uid, id.toString)
    tree.addAttribute(XProcConstants._type, stepType.toString)
    if (stepName.isDefined) {
      tree.addAttribute(XProcConstants._name, stepName.get)
    }

    if (defaultReadableStep.isDefined) {
      tree.addAttribute(new QName("", "default-readable-step"), defaultReadableStep.get.id.toString)
    }

    if (inputs.isDefined) {
      inputs.get.foreach { _.dump(tree) }
    }
    if (options.isDefined) {
      options.get.foreach { _.dump(tree) }
    }
    //tree.addStartElement(XProcConstants.px("default-readable-step"))

    tree.addEndElement()
  }
}
