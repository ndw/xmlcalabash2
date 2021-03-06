package com.xmlcalabash.model.xml

import com.jafpl.graph.Node
import com.xmlcalabash.config.{StepSignature, XMLCalabashConfig}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.{XMLCalabashRuntime, XProcXPathExpression}
import net.sf.saxon.s9api.SaxonApiException

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Container(override val config: XMLCalabashConfig) extends Step(config) with NamedArtifact {
  protected var _outputs = mutable.HashMap.empty[String, DeclareOutput]

  def atomic: Boolean = {
    for (child <- allChildren) {
      child match {
        case _: AtomicStep =>
          // Synthetic children (inline-loader, etc.) don't count
          if (!child.synthetic) {
            return false
          }
        case _: Container => return false
        case _ => ()
      }
    }
    true
  }

  protected[model] def makeContainerStructureExplicit(): Unit = {
    var firstChild = Option.empty[Artifact]
    var withInput = Option.empty[WithInput]
    var lastOutput = Option.empty[DeclareOutput]
    var primaryOutput = Option.empty[DeclareOutput]
    var lastStep = Option.empty[Step]

    for (child <- allChildren) {
      child.makeStructureExplicit()

      if (firstChild.isEmpty) {
        firstChild = Some(child)
      }

      child match {
        case input: WithInput =>
          if (withInput.isDefined) {
            throw new RuntimeException("Only one with-input is allowed")
          }
          withInput = Some(input)
        case output: DeclareOutput =>
          if (_outputs.contains(output.port)) {
            throw new RuntimeException("duplicate output port")
          }
          _outputs.put(output.port, output)

          lastOutput = Some(output)
          if (output.primary) {
            if (primaryOutput.isDefined) {
              throw XProcException.xsDupPrimaryPort(output.port, primaryOutput.get.port, staticContext.location)
            }
            primaryOutput = Some(output)
          }
        case atomic: AtomicStep =>
          lastStep = Some(atomic)
        case compound: Container =>
          lastStep = Some(compound)
        case variable: Variable =>
          environment().addVariable(variable)
        case _ =>
          ()
      }
    }

    if (_outputs.isEmpty && lastStep.isDefined && lastStep.get.primaryOutput.isDefined) {
      val output = syntheticDeclaredOutput()

      val pipe = new Pipe(config)
      pipe.step = lastStep.get.stepName
      pipe.port = lastStep.get.primaryOutput.get.port
      pipe.link = lastStep.get.primaryOutput.get
      output.addChild(pipe)

      if (firstChild.isDefined) {
        addChild(output, firstChild.get)
      } else {
        addChild(output)
      }
    }

    if (_outputs.size == 1 && lastOutput.get._primary.isEmpty) {
      lastOutput.get.primary = true
    }
  }

  protected def syntheticDeclaredOutput(): DeclareOutput = {
    val output = new DeclareOutput(config)
    output.port = "#result"
    output.primary = true
    output.sequence = true
    output
  }

  override protected[model] def makeBindingsExplicit(): Unit = {
    val env = environment()

    // Be careful here, we don't call super.makeBindingsExplicit() so this method
    // has to be kept up-to-date with respect to changes there!

    // Make the bindings for this step's inputs explicit
    for (input <- children[WithInput]) {
      input.makeBindingsExplicit()
    }

    for (sbinding <- env.staticVariables) {
      _inScopeStatics.put(sbinding.name.getClarkName, sbinding)
    }

    for (dbinding <- env.variables) {
      _inScopeDynamics.put(dbinding.name, dbinding)
    }

    if (depends.nonEmpty) {
      val env = environment()
      for (stepName <- depends) {
        val step = env.step(stepName)
        if (step.isEmpty) {
          throw XProcException.xsNotAStep(stepName, location)
        } else {
          dependSteps += step.get
        }
      }
    }

    if (atomic) {
      return
    }

    var poutput = Option.empty[DeclareOutput]
    var lastStep = Option.empty[Step]
    for (child <- allChildren) {
      child match {
        case output: DeclareOutput =>
          if (output.primary) {
            poutput = Some(output)
          }
        case step: Step =>
          lastStep = Some(step)
        case _ => ()
      }
    }

    if (lastStep.isDefined && poutput.isDefined && poutput.get.bindings.isEmpty) {
      val lpo = lastStep.get.primaryOutput
      if (lpo.isDefined) {
        val pipe = new Pipe(config)
        pipe.port = lpo.get.port
        pipe.step = lastStep.get.stepName
        pipe.link = lpo.get
        poutput.get.addChild(pipe)
      }
    }

    for (child <- allChildren) {
      child match {
        case option: DeclareOption =>
          option.makeBindingsExplicit()
          if (option.select.isDefined && !option.static) {
            option.staticValue = computeStatically(option.select.get)
          }
        case _: WithInput =>
          () // we did this above
        case _ =>
          child.makeBindingsExplicit()
      }
    }
  }

  override protected[model] def validateStructure(): Unit = {
    for (child <- allChildren) {
      child.validateStructure()
      child match {
        case _: WithInput => ()
        case _: WithOutput => ()
        case _: DeclareInput => ()
        case _: DeclareOutput => ()
        case _: Step => ()
        case _: NamePipe => () // For Viweport
        case _ =>
          throw new RuntimeException(s"Unexpected content in $this: $child")
      }
    }
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    if (allChildren.nonEmpty) {
      if (_graphNode.isDefined) {
        for (child <- allChildren) {
          child match {
            case step: Step =>
              step.graphNodes(runtime, _graphNode.get)
            case option: DeclareOption =>
              option.graphNodes(runtime, _graphNode.get)
            case variable: Variable =>
              variable.graphNodes(runtime, _graphNode.get)
            case _ => ()
          }
        }
      } else {
        println("cannot graphNodes for children of " + this)
      }
    }
  }

}
