package com.xmlcalabash.model.xml

import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

// You'd think that the environment just inherited down the tree as you went,
// but it's more complicated than that. What's in and out of the environment
// is quite different inside a compound step than it is outside. So instead of
// trying to finesse it as we walk down the tree, this object computes the
// environment for the step identified in its constructor.
//
// N.B. Do not cache this; you'd think it would be a performance enhancement,
// but dealing with select expressions sometimes adds filters and that
// changes the default readable port.

object Environment {
  def newEnvironment(step: Artifact): Environment = {
    // Walk "up" the tree until we find a valid starting point
    step match {
      case step: Step => Unit
      case input: DeclareInput => Unit
      case output: DeclareOutput => Unit
      case variable: Variable => Unit
      case option: DeclareOption => Unit
      case _ =>
        return Environment.newEnvironment(step.parent.get)
    }

    val env = new Environment()

    val ancestors = ListBuffer.empty[Artifact]    // includes self for compound steps
    var pptr: Option[Artifact] = Some(step)
    while (pptr.isDefined) {
      pptr.get match {
        case variable: Variable =>
          ancestors.insert(0, variable)
        case option: DeclareOption =>
          ancestors.insert(0, option)
        case input: DeclareInput =>
          ancestors.insert(0, input)
        case output: DeclareOutput =>
          ancestors.insert(0, output)
        case library: Library =>
          ancestors.insert(0, library)
        case step: Step =>
          ancestors.insert(0, step)
        case _ => Unit
      }

      pptr = pptr.get.parent
    }

    val x = walk(env, ancestors.toList)
    //println(s"$step = $x")
    x
  }

  private def walk(env: Environment, ancestors: List[Artifact]): Environment = {
    if (ancestors.isEmpty) {
      return env
    }

    val head = ancestors.head
    val next = ancestors.tail.headOption

    if (head.isInstanceOf[DeclareStep]) {
      // The only thing that survives passing into a declare step are statics
      env.clearSteps()
      env.clearPorts()
      env.clearDynamicVariables()
    }

    head match {
      case lib: Library =>
        env.defaultReadablePort = None

        // Libraries are a special case, they aren't in the children of the container
        if (next.get.isInstanceOf[Library]) {
          return walk(env, ancestors.tail)
        }

        // Now walk down to the next ancestor
        for (child <- lib.allChildren) {
          if (next.get == child) {
            return walk(env, ancestors.tail)
          }

          child match {
            case variable: Variable =>
              env.addVariable(variable)
            case option: DeclareOption =>
              env.addVariable(option)
            case childstep: Step =>
              if (next.isDefined && next.get == childstep) {
                return walk(env, ancestors.tail)
              }
            case _ => Unit
          }
        }

        // If we fell off the bottom of this loop, something has gone terribly wrong
        throw new RuntimeException("Fell of ancestor list in computing environment")

      case step: Container =>
        // DeclareStep is special; if it's the last ancestor, then it's the root of
        // the pipeline and that doesn't get to read its own inputs. If it's not
        // the root, then we're setting up the environment for one of its contained
        // steps.

        // This step is in the environment
        env.addStep(step)

        if (next.isDefined) {
          // Its inputs are readable
          for (port <- step.children[DeclareInput]) {
            if (port.primary) {
              env.defaultReadablePort =  port
            }
            env.addPort(port)
          }

          // Its options are in-scope
          for (option <- step.children[DeclareOption]) {
            env.addVariable(option)
          }

          step match {
            // Choose, when, etc., aren't ordinary container steps
            case container: Choose => Unit
            //case container: When => Unit
            //case container: Otherwise => Unit
            case container: Try => Unit
            //case container: Catch => Unit
            //case container: Finally => Unit
            case _ =>
              // Entering a declare-step resets the default readable port
              if (step.isInstanceOf[DeclareStep]) {
                env.defaultReadablePort = step.primaryInput
              }

              // The outputs of all contained steps are mutually readable
              for (child <- step.allChildren) {
                child match {
                  case decl: DeclareStep => Unit // these don't count
                  case childstep: Container =>
                    if (next.isDefined && next.get == childstep) {
                      // ignore this one, we'll be diving down into it
                    } else {
                      env.addStep(childstep)
                      for (port <- childstep.children[WithOutput]) {
                        env.addPort(port)
                      }
                    }
                  case childstep: Step =>
                    // Yes, this can add the output of the step who's environment
                    // we're computing to the list of readable ports. Doing so
                    // is a loop, it'll be caught elsewhere.
                    env.addStep(childstep)
                    for (port <- childstep.children[WithOutput]) {
                      env.addPort(port)
                    }
                  case _ => Unit
                }
              }
          }
        }

        if (next.isEmpty) {
          return env
        }

        // Libraries are a special case, they aren't in the children of the container
        if (next.get.isInstanceOf[Library]) {
          return walk(env, ancestors.tail)
        }

        // Now walk down to the next ancestor, calculating the drp
        for (child <- step.allChildren) {
          if (next.get == child) {
            return walk(env, ancestors.tail)
          }
          step match {
            // The children of choose and try aren't ordinary children
            case container: Choose => Unit
            case container: Try => Unit
            case _ =>
              child match {
                case option: DeclareOption =>
                  env.addVariable(option)
                case variable: Variable =>
                  env.addVariable(variable)
                case decl: DeclareStep =>
                  Unit
                case childstep: Step =>
                  env.defaultReadablePort = childstep.primaryOutput
                case _ => Unit
              }
          }
        }

        // If we fell off the bottom of this loop, something has gone terribly wrong
        throw new RuntimeException("Fell of ancestor list in container")

      case step: AtomicStep =>
        if (next.isEmpty) {
          // This is us.
          return env
        }

        // If we got here, something has gone terribly wrong
        throw new RuntimeException("Atomic step with children?")

      case option: DeclareOption =>
        if (next.isEmpty) {
          // This is us.
          return env
        }

        // If we got here, something has gone terribly wrong
        throw new RuntimeException("Option with children?")

      case variable: Variable =>
        if (next.isEmpty) {
          // This is us.
          return env
        }

        // If we got here, something has gone terribly wrong
        throw new RuntimeException("Variable with children?")

      case input: DeclareInput =>
        if (next.isEmpty) {
          // This is us.
          return env
        }

        // If we got here, something has gone terribly wrong
        throw new RuntimeException("Input with children?")

      case input: DeclareOutput =>
        if (next.isEmpty) {
          // This is us.
          return env
        }

        // If we got here, something has gone terribly wrong
        throw new RuntimeException("Output with children?")

      case _ => throw new RuntimeException(s"Unexpected in list of ancestors: $head")
    }
  }
}

class Environment private() {
  private val _inScopeSteps = mutable.HashMap.empty[String, Step]
  private val _inScopePorts = mutable.HashMap.empty[String, Port]
  private val _inScopeVariables = mutable.HashMap.empty[QName,NameBinding]
  private var _defaultReadablePort = Option.empty[Port]

  def defaultReadablePort: Option[Port] = _defaultReadablePort
  protected[xml] def defaultReadablePort_=(port: Port): Unit = {
    defaultReadablePort = Some(port)
  }
  protected[xml] def defaultReadablePort_=(port: Option[Port]): Unit = {
    _defaultReadablePort = port
  }
  private def clearSteps(): Unit = {
    _inScopeSteps.clear()
  }

  def addStep(step: Step): Unit = {
    addName(step.stepName, step)
  }

  private def clearPorts(): Unit = {
    _inScopePorts.clear()
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

  private def clearVariables(): Unit = {
    _inScopeVariables.clear()
  }

  private def clearDynamicVariables(): Unit = {
    val statics = mutable.HashMap.empty[QName,NameBinding]
    for (static <- staticVariables) {
      statics.put(static.name, static)
    }
    _inScopeVariables.clear()
    _inScopeVariables ++= statics
  }


  def step(name: String): Option[Step] = _inScopeSteps.get(name)

  def port(stepName: String, portName: String): Option[Port] = {
    if (step(stepName).isDefined) {
      _inScopePorts.get(s"$stepName/$portName")
    } else {
      None
    }
  }

  def variable(name: QName): Option[NameBinding] = {
    if (_inScopeVariables.contains(name)) {
      _inScopeVariables.get(name)
    } else {
      None
    }
  }

  def variables: List[NameBinding] = {
    val lb = ListBuffer.empty[NameBinding]
    for ((name, variable) <- _inScopeVariables) {
      if (!variable.static) {
        lb += variable
      }
    }
    lb.toList
  }

  def staticVariables: List[NameBinding] = {
    val lb = ListBuffer.empty[NameBinding]
    for ((name, variable) <- _inScopeVariables) {
      if (variable.static) {
        lb += variable
      }
    }
    lb.toList
  }
}
