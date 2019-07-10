package com.xmlcalabash.model.xml

import com.jafpl.graph.Node
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.{XMLCalabashRuntime, XProcXPathExpression}

import scala.collection.mutable

class Container(override val config: XMLCalabashConfig) extends Step(config) with NamedArtifact {
  protected var _outputs = mutable.HashMap.empty[String, DeclareOutput]

  def atomic: Boolean = {
    for (child <- allChildren) {
      child match {
        case atomic: AtomicStep => return false
        case compound: Container => return false
        case _ => Unit
      }
    }
    true
  }

  protected[model] def makeContainerStructureExplicit(environment: Environment): Unit = {
    var firstChild = Option.empty[Artifact]
    var withInput = Option.empty[WithInput]
    var lastOutput = Option.empty[DeclareOutput]
    var primaryOutput = Option.empty[DeclareOutput]
    var lastStep = Option.empty[Step]

    for (child <- allChildren) {
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
          atomic.makeStructureExplicit(environment)
          lastStep = Some(atomic)
        case compound: Container =>
          compound.makeStructureExplicit(environment)
          lastStep = Some(compound)
        case variable: Variable =>
          variable.makeStructureExplicit(environment)
          environment.addVariable(variable)
        case _ =>
          child.makeStructureExplicit(environment)
      }
    }

    if (_outputs.isEmpty && lastStep.isDefined && lastStep.get.primaryOutput.isDefined) {
      val output = new DeclareOutput(config)
      output.port = "#result"
      output.primary = true
      output.sequence = true

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

  protected[model] def configureContainerEnvironment(env: Environment): Environment = {
    // Add the readable steps to the environment
    val containerEnvironment = new Environment(env)
    for (port <- children[DeclareInput]) {
      containerEnvironment.addPort(port)
    }
    for (step <- children[Step]) {
      containerEnvironment.addStep(step)
      for (port <- step.children[DeclareOutput]) {
        containerEnvironment.addPort(port)
      }
    }
    containerEnvironment
  }

  override protected[model] def makeBindingsExplicit(initialEnvironment: Environment, initialDrp: Option[Port]): Unit = {
    // Be careful here, we don't call super.makeBindingsExplicit() so this method
    // has to be kept up-to-date with respect to changes there!

    val containerEnvironment = configureContainerEnvironment(initialEnvironment)
    // Make the bindings for this step's inputs explicit
    for (input <- children[WithInput]) {
      input.makeBindingsExplicit(containerEnvironment, initialDrp)
    }

    _drp = initialDrp

    for (sbinding <- initialEnvironment.staticVariables) {
      _inScopeStatics.put(sbinding.name.getClarkName, sbinding)
    }

    for (dbinding <- initialEnvironment.variables) {
      _inScopeDynamics.put(dbinding.name, dbinding)
    }

    if (atomic) {
      return
    }

    var poutput = Option.empty[DeclareOutput]
    var drp = initialDrp
    var lastStep = Option.empty[Step]
    for (child <- allChildren) {
      child match {
        case input: DeclareInput =>
          if (input.primary) {
            drp = Some(input)
          }
        case output: DeclareOutput =>
          if (output.primary) {
            poutput = Some(output)
          }
        case step: Step =>
          lastStep = Some(step)
        case _ => Unit
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
        case decl: DeclareStep =>
          val newenvironment = containerEnvironment.declareStep()
          decl.makeBindingsExplicit(newenvironment)
        case step: Step =>
          step.makeBindingsExplicit(containerEnvironment, drp)
          drp = step.primaryOutput
        case variable: Variable =>
          variable.makeBindingsExplicit(containerEnvironment, drp)
          containerEnvironment.addVariable(variable)
        case option: DeclareOption =>
          option.makeBindingsExplicit(containerEnvironment, drp)
          if (option.select.isDefined && !option.static) {
            // Evaluate it; no reference to context is allowed.
            val exprContext = staticContext.withStatics(inScopeStatics)
            val expr = new XProcXPathExpression(staticContext, option.select.get)
            val msg = config.expressionEvaluator.value(expr, List(), exprContext.statics, None)
            option.staticValue = msg
          }
          containerEnvironment.addVariable(option)
        case input: WithInput =>
          Unit // we did this above
        case _ =>
          child.makeBindingsExplicit(containerEnvironment, drp)
      }
    }
  }

  override protected[model] def validateStructure(): Unit = {
    for (child <- allChildren) {
      child.validateStructure()
      child match {
        case art: WithInput => Unit
        case art: WithOutput => Unit
        case art: DeclareInput => Unit
        case art: DeclareOutput => Unit
        case art: Step => Unit
        case art: NamePipe => Unit // For Viweport
        case _ =>
          throw new RuntimeException(s"Unexpected content in $this: $child")
      }
    }
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node) {
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
            case _ => Unit
          }
        }
      } else {
        println("cannot graphNodes for children of " + this)
      }
    }
  }

}
