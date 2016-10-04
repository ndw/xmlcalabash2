package com.xmlcalabash.model.xml

import com.xmlcalabash.core.{XProcConstants, XProcEngine}
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.{QName, XdmNode}

/**
  * Created by ndw on 9/30/16.
  */
class StepDeclaration(context: Option[XdmNode]) extends Step(context) {
  private var _stepType: Option[QName] = None
  private var _inputs: Option[List[InputDeclaration]] = None
  private var _outputs: Option[List[OutputDeclaration]] = None
  private var _options: Option[List[OptionDeclaration]] = None
  private var _declaredSteps: Option[List[StepDeclaration]] = None
  private var _imports: Option[List[Import]] = None
  private var _subpipeline: Option[List[Artifact]] = None

  def stepType = _stepType
  def inputs = _inputs
  def outputs = _outputs
  def options = _options
  def declaredSteps = _declaredSteps
  def imports = _imports
  def subpipeline = _subpipeline

  def stepType_=(value: Option[QName]): Unit = {
    _stepType = value
  }

  // FIXME: the rest of the setters

  def addInput(input: InputDeclaration): Unit = {
    if (_inputs.isDefined) {
      for (curInput <- _inputs.get) {
        if (curInput.port == input.port) {
          staticError("Cannot declare two input ports with the same name: " + input.port)
        }
      }
      _inputs = Some(_inputs.get ::: List(input))
    } else {
      _inputs = Some(List(input))
    }
  }

  def addOutput(output: OutputDeclaration): Unit = {
    if (_outputs.isDefined) {
      for (curOutput <- _outputs.get) {
        if (curOutput.port == output.port) {
          staticError("Cannot declare two output ports with the same name: " + output.port)
        }
      }
      _outputs = Some(_outputs.get ::: List(output))
    } else {
      _outputs = Some(List(output))
    }
  }

  def addOption(option: OptionDeclaration): Unit = {
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

  def addDeclaredStep(step: StepDeclaration): Unit = {
    if (step.stepType.isEmpty) {
      staticError("Nested declared steps must have a type")
    }

    if (_declaredSteps.isDefined) {
      for (curStep <- _declaredSteps.get) {
        if (curStep.stepType.get.equals(step.stepType.get)) {
          staticError("Cannot declare sibling steps with the same type: " + step.stepType.get)
        }
      }
      _declaredSteps = Some(_declaredSteps.get ::: List(step))
    } else {
      _declaredSteps = Some(List(step))
    }
  }

  def addImport(anImport: Import): Unit = {
    if (_imports.isDefined) {
      _imports = Some(_imports.get ::: List(anImport))
    } else {
      _imports = Some(List(anImport))
    }
  }

  def addSubpipeline(step: Artifact): Unit = {
    val checked: Option[Artifact] = step match {
      case x: StepInstance => Some(x)
      // FIXME: variables go here
      case _ =>
        staticError("Only variables and steps may go in a subpipeline")
        None
    }

    if (_subpipeline.isDefined) {
      _subpipeline = Some(_subpipeline.get ::: List(checked.get))
    } else {
      _subpipeline = Some(List(checked.get))
    }
  }

  def isAtomic: Boolean = {
    _subpipeline.isEmpty
  }

  def defaultReadablePort: Option[InputDeclaration] = {
    if (inputs.isDefined) {
      for (input <- inputs.get) {
        if (input.primary.isDefined && input.primary.get) {
          return Some(input)
        }
      }
    }
    None
  }

  def instanceDefaultReadablePort: Option[OutputDeclaration] = {
    if (outputs.isDefined) {
      for (output <- outputs.get) {
        if (output.primary.isDefined && output.primary.get) {
          return Some(output)
        }
      }
    }
    None
  }

  def valid(instance: StepInstance): Boolean = {
    true
  }

  def dump(engine: XProcEngine): XdmNode = {
    val tree = new TreeWriter(engine)
    tree.startDocument(null)
    dump(tree)
    tree.getResult
  }

  override def dump(tree: TreeWriter): Unit = {
    tree.addStartElement(XProcConstants.px("declare-step"))
    tree.addAttribute(XProcConstants._uid, id.toString)

    if (stepName.isDefined) {
      tree.addAttribute(XProcConstants._type, stepType.toString)
    }
    if (stepName.isDefined) {
      tree.addAttribute(XProcConstants._name, stepName.get)
    }
    if (inputs.isDefined) {
      inputs.get.foreach { _.dump(tree) }
    }
    if (outputs.isDefined) {
      outputs.get.foreach { _.dump(tree) }
    }
    if (options.isDefined) {
      options.get.foreach { _.dump(tree) }
    }
    if (subpipeline.isDefined) {
      subpipeline.get.foreach { _.dump(tree) }
    }
    tree.addEndElement()
  }

}


