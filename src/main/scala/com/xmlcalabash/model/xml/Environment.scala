package com.xmlcalabash.model.xml

import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Environment(val parent: Option[Environment]) {
  def this() {
    this(None)
  }

  def this(env: Environment) {
    this(Some(env))
  }

  private val _inScopeSteps = mutable.HashMap.empty[String, Step]
  private val _inScopePorts = mutable.HashMap.empty[String, Port]
  private val _inScopeVariables = mutable.HashMap.empty[QName,NameBinding]

  def declareStep(): Environment = {
    // If this is an environment for a declare step; the only
    // thing that should be inherited are the static variables.
    val env = new Environment()
    for (static <- staticVariables) {
      env.addVariable(static)
    }
    env
  }

  def addStep(step: Step): Unit = {
    if (!step.stepName.startsWith("!")) {
      addName(step.stepName, step)
    }
  }

  def addPort(port: Port): Unit = {
    val name = port.parent.get match {
      case step: Step => step.stepName
      case _ =>
        throw new RuntimeException("Parent of port isn't a step?")
    }
    if (!name.startsWith("!")) {
      _inScopePorts.put(s"$name/${port.port}", port)
    }
  }

  private def addName(name: String, step: Step): Unit = {
    if (_inScopeSteps.contains(name)) {
      throw new RuntimeException("duplicate name in scope")
    }
    _inScopeSteps.put(name, step)
  }

  def addVariable(binding: NameBinding): Unit = {
    _inScopeVariables.put(binding.name, binding)
  }

  def step(name: String): Option[Step] = {
    if (_inScopeSteps.contains(name)) {
      _inScopeSteps.get(name)
    } else {
      if (parent.isDefined) {
        parent.get.step(name)
      } else {
        None
      }
    }
  }

  def port(stepName: String, portName: String): Option[Port] = {
    if (step(stepName).isDefined) {
      _inScopePorts.get(s"$stepName/$portName")
    } else {
      if (parent.isDefined) {
        parent.get.port(stepName, portName)
      } else {
        None
      }
    }
  }

  def variable(name: QName): Option[NameBinding] = {
    if (_inScopeVariables.contains(name)) {
      _inScopeVariables.get(name)
    } else {
      if (parent.isDefined) {
        parent.get.variable(name)
      } else {
        None
      }
    }
  }

  def variables: List[NameBinding] = {
    val lb = ListBuffer.empty[NameBinding]
    // This *shouldn't* consider parent contexts, right?
    for ((name, variable) <- _inScopeVariables) {
      if (!variable.static) {
        lb += variable
      }
    }
    lb.toList
  }

  def staticVariables: List[NameBinding] = {
    val lb = ListBuffer.empty[NameBinding]
    if (parent.isDefined) {
      lb ++= parent.get.staticVariables
    }
    for ((name, variable) <- _inScopeVariables) {
      if (variable.static) {
        lb += variable
      }
    }
    lb.toList
  }
}
