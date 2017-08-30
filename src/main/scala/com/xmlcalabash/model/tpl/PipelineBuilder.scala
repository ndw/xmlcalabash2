package com.xmlcalabash.model.tpl

import com.xmlcalabash.exceptions.ModelException
import com.xmlcalabash.model.tpl.TplParser.EventHandler
import com.xmlcalabash.model.tpl.containers.{Choose, Container, Group, Otherwise, When}
import com.xmlcalabash.model.util.ParserConfiguration

import scala.collection.mutable

class PipelineBuilder(config: ParserConfiguration) extends EventHandler {
  private var input: String = null

  val stack: mutable.ListBuffer[Artifact] = mutable.ListBuffer.empty[Artifact]

  def reset(string: String) {
    input = string
  }

  def startNonterminal(name: String, begin: Int) {
    name match {
      case "Pipeline" =>
        val pipe = new Pipeline(config, None)
        stack += pipe
        println("PUSH " + pipe)
      case "Cut" =>
        val cut = new Cut(config, Some(stack.last))
        stack += cut
        println("PUSH " + cut)
      case "Step" =>
        val step = new Step(config, Some(stack.last))
        stack += step
        println("PUSH " + step)
      case "AtomicStep" => startStep("atomic")
      case "CompoundStep" => Unit
      case "CompoundBody" => Unit
      case "Group" => startStep("group")
      case "Choose" => startStep("choose")
      case "When" => startStep("when")
      case "Otherwise" => startStep("otherwise")
      case "PortMap" => Unit
      case "BindingList" => Unit
      case "PortBinding" => Unit
      case "SourceBindingList" =>
        val art = new SourceBindingList(config, None)
        stack += art
        println("PUSH " + art)
      case "ResultBindingList" =>
        val art = new ResultBindingList(config, None)
        stack += art
        println("PUSH " + art)
      case "Opts" => Unit
      case "OptionBinding" => Unit
      case _ =>
        throw new ModelException("badnt", s"Start of unexpected non-terminal: $name")
    }
  }

  def endNonterminal(name: String, end: Int) {
    name match {
      case "Pipeline" =>
        if (stack.isEmpty) {
          throw new ModelException("stack", s"Stack empty at end of cut?")
        }


        val pipe = stack.last match {
          case art: Pipeline =>
            art
          case _ => throw new ModelException("badnt", s"Unexpected stack at end of pipeline: ${stack.last}")
        }
        stack.remove(stack.size - 1)
        println("POP " + pipe + ": " + stack.size)

        if (stack.nonEmpty) {
          throw new ModelException("stack", s"Stack is not empty after Pipeline: ${stack.size}")
        }
      case "Cut" =>
        if (stack.isEmpty) {
          throw new ModelException("stack", s"Stack empty at end of cut?")
        }

        val cut = stack.last match {
          case art: Cut =>
            art
          case _ => throw new ModelException("badnt", s"Unexpected stack at end of cut: ${stack.last}")
        }
        stack.remove(stack.size - 1)
        println("POP " + cut + ": " + stack.size)

        stack.last match {
          case container: Container =>
            container.addCut(cut)
          case _ => throw new ModelException("badnt", s"Unexpected stack at end of cut: ${stack.last}")
        }
      case "Step" => Unit
      case "AtomicStep" => endStep("atomic")
      case "CompoundStep" => Unit
      case "CompoundBody" => Unit
      case "Group" => endStep("group")
      case "Choose" => endStep("choose")
      case "When" => endStep("when")
      case "Otherwise" => endStep("otherwise")
      case "PortMap" => Unit
      case "BindingList" => Unit
      case "SourceBindingList" =>
        if (stack.isEmpty) {
          throw new ModelException("stack", s"Stack empty at end of source binding list?")
        }
        val item = stack.last match {
          case art: SourceBindingList =>
            art
          case _ => throw new ModelException("badnt", s"Unexpected stack at end of source binding list: ${stack.last}")
        }
        stack.remove(stack.size - 1)
        println("POP " + item + ": " + stack.size)
      case "ResultBindingList" =>
        if (stack.isEmpty) {
          throw new ModelException("stack", s"Stack empty at end of result binding list?")
        }
        val item = stack.last match {
          case art: ResultBindingList =>
            art
          case _ => throw new ModelException("badnt", s"Unexpected stack at end of result binding list: ${stack.last}")
        }
        stack.remove(stack.size - 1)
        println("POP " + item + ": " + stack.size)
      case "PortBinding" =>
        if (stack.size < 3) {
          throw new ModelException("stack", s"Stack too short at end of port binding?")
        }

        var binding = stack.last match {
          case terminal: Terminal =>
            terminal
          case _ => throw new ModelException("badnt", s"Unexpected stack at end of port binding: ${stack.last}")
        }
        stack.remove(stack.size - 1)

        var portname = stack.last match {
          case terminal: Terminal =>
            terminal
          case _ => throw new ModelException("badnt", s"Unexpected stack at end of port binding: ${stack.last}")
        }
        stack.remove(stack.size - 1)

        var sourceBinding = stack.last match {
          case source: SourceBindingList => true
          case _ => false
        }

        stack(stack.size - 2) match {
          case step: Step =>
            val pbind = if (binding.name == "AnyName") {
              new PortRefBinding(portname.text, binding.text)
            } else {
              new PortLitBinding(portname.text, binding.text)
            }
            if (sourceBinding) {
              step.addSourceBinding(pbind)
            } else {
              step.addResultBinding(pbind)
            }
          case _ => throw new ModelException("badnt",
            s"Unexpected stack for port binding list: ${stack(stack.size - 2)}")

        }
      case "Opts" => Unit
      case "OptionBinding" =>
        if (stack.size < 3) {
          throw new ModelException("stack", s"Stack too short at end of port binding?")
        }

        var binding = stack.last match {
          case terminal: Terminal =>
            terminal
          case _ => throw new ModelException("badnt", s"Unexpected stack at end of port binding: ${stack.last}")
        }
        stack.remove(stack.size - 1)

        var varname = stack.last match {
          case terminal: Terminal =>
            terminal
          case _ => throw new ModelException("badnt", s"Unexpected stack at end of port binding: ${stack.last}")
        }
        stack.remove(stack.size - 1)

        stack.last match {
          case step: Step =>
            val optbind = new OptionBinding(varname.text, binding.text)
            step.addOptionBinding(optbind)
          case _ => throw new ModelException("badnt",
            s"Unexpected stack for port binding list: ${stack(stack.size - 2)}")

        }
      case _ =>
        throw new ModelException("badnt", s"End of unexpected non-terminal: $name")
    }
  }

  def terminal(name: String, begin: Int, end: Int) {
    val tag = if (name(0) == '\'') "TOKEN" else name
    val text = characters(begin, end)

    tag match {
      case "TOKEN" => Unit
      case "StepName" =>
        if (stack.isEmpty) {
          throw new ModelException("stack", s"Stack empty at StepName?")
        }

        val step = stack.last match {
          case step: AtomicStep =>
            step
          case _ => throw new ModelException("badnt", s"Unexpected stack StepName: ${stack.last}")
        }

        step.name = text
      case "AnyName" =>
        stack += new Terminal(config, None, tag, text)
      case "StringLiteral" =>
        stack += new Terminal(config, None, tag, text)
      case "EOF" => Unit
      case _ =>
        println("Unexpected terminal: " + tag + ": " + text)
    }
  }

  def whitespace(begin: Int, end: Int) {
    // nop
  }

  private def characters(begin: Int, end: Int): String = {
    if (begin < end) {
      input.substring(begin, end)
    } else {
      ""
    }
  }

  private def startStep(name: String): Unit = {
    // This should replace the placeholder step on the stack
    if (stack.isEmpty) {
      throw new ModelException("stack", s"Stack empty at start of $name?")
    }

    val step = stack.last match {
      case step: Choose =>
        step
      case step: Step =>
        stack.remove(stack.size - 1)
        step
      case _ => throw new ModelException("badnt", s"Unexpected stack at start of $name: ${stack.last}")
    }

    val astep = name match {
      case "atomic" => new AtomicStep(config, Some(stack.last))
      case "group" => new Group(config, Some(stack.last))
      case "choose" => new Choose(config, Some(stack.last))
      case "when" => new When(config, Some(stack.last))
      case "otherwise" => new Otherwise(config, Some(stack.last))
      case _ => throw new ModelException("badtype", s"Unknown container type: $name")
    }

    astep.sourceBindings = step.sourceBindings
    astep.resultBindings = step.resultBindings
    astep.optionBindings = step.optionBindings
    stack += astep
    println("PUSH " + astep)
  }

  private def endStep(name: String): Unit = {
    if (stack.isEmpty) {
      throw new ModelException("stack", s"Stack empty at end of $name?")
    }

    val step = stack.last match {
      case step: Step =>
        step
      case _ => throw new ModelException("badnt", s"Unexpected stack at end of $name: ${stack.last}")
    }
    stack.remove(stack.size - 1)
    println("POP " + step + ": " + stack.size)

    stack.last match {
      case cut: Cut =>
        cut.addStep(step)
      case container: Choose =>
        step match {
          case child: When =>
            container.addWhen(child)
          case child: Otherwise =>
            container.addOtherwise(child)
          case _ => throw new ModelException("badnt", s"Unexpected child of choose $step")
        }
      case _ => throw new ModelException("badnt", s"Unexpected stack at end of $name: ${stack.last}")
    }
  }
}
