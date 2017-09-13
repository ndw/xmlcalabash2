package com.xmlcalabash.model.xml.containers

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.xml.datasource.Pipe
import com.xmlcalabash.model.xml.{Artifact, Input, Output, PipelineStep, Variable}

import scala.collection.mutable

class Container(override val config: XMLCalabash,
                override val parent: Option[Artifact]) extends PipelineStep(config, parent) {
  protected var _name: Option[String] = None

  def firstChildStep: Option[PipelineStep] = {
    for (child <- children) {
      child match {
        case step: PipelineStep =>
          return Some(step)
        case _ => Unit
      }
    }
    None
  }

  def lastChildStep: Option[PipelineStep] = {
    var last = Option.empty[PipelineStep]
    for (child <- children) {
      child match {
        case step: PipelineStep =>
          last = Some(step)
        case _ => Unit
      }
    }
    last
  }

  override def makeInputPortsExplicit(): Boolean = {
    var primary = Option.empty[Input]
    val ports = mutable.HashSet.empty[String]
    for (child <- children) {
      child match {
        case input: Input =>
          if (ports.contains(input.port.get)) {
            throw new ModelException(ExceptionCode.DUPCONTAINERINPUTPORT, input.port.get, location)
          }
          ports += input.port.get

          if (input.primary) {
            if (primary.isDefined) {
              throw new ModelException(ExceptionCode.DUPPRIMARYINPUT, List(input.port.get, primary.get.port.get), location)
            }
            primary = Some(input)
          }
        case _ => Unit
      }
    }

    if (primary.isEmpty && (inputPorts.size == 1)) {
      primary = input(inputPorts.head)
      primary.get.primary = true
    }

    valid
  }

  override def makeOutputPortsExplicit(): Boolean = {
    var primary = Option.empty[Output]
    val ports = mutable.HashSet.empty[String]
    for (child <- children) {
      child match {
        case output: Output =>
          if (ports.contains(output.port.get)) {
            throw new ModelException(ExceptionCode.DUPCONTAINEROUTPUTPORT, output.port.get, location)
          }
          ports += output.port.get

          if (output.primary) {
            if (primary.isDefined) {
              throw new ModelException(ExceptionCode.DUPPRIMARYINPUT, List(output.port.get, primary.get.port.get), location)
            }
            primary = Some(output)
          }
        case _ => Unit
      }
    }

    if (outputPorts.isEmpty) {
      val step = lastChildStep
      if (step.isDefined && step.get.primaryOutput.isDefined) {
        val output = new Output(config, this, "#result", primary=true, sequence=step.get.primaryOutput.get.sequence)
        addChild(output)
      }
    } else {
      if (primary.isEmpty && (outputPorts.size == 1)) {
        primary = output(outputPorts.head)
        primary.get.primary = true
      }
    }

    valid
  }

  override def makePortsExplicit(): Boolean = {
    for (child <- children) {
      child match {
        case step: PipelineStep =>
          step.makePortsExplicit()
        case _ => Unit
      }
    }
    valid = valid && makeInputPortsExplicit() && makeOutputPortsExplicit()
    valid
  }

  override def makeOutputBindingsExplicit(): Boolean = {
    for (port <- outputPorts) {
      val out = output(port).get
      val sources = out.dataSources
      if (sources.isEmpty) {
        val last = lastChildStep
        if (last.isDefined) {
          var output = Option.empty[Output]
          for (oport <- last.get.outputPorts) {
            if (last.get.output(oport).get.primary) {
              output = last.get.output(oport)
            }
          }
          if (output.isDefined) {
            val pipe = new Pipe(config, out, last.get.name, output.get.port.get)
            out.addChild(pipe)
          } else {
            throw new ModelException(ExceptionCode.NOCONTAINEROUTPUT, port, location)
          }
        } else {
          throw new ModelException(ExceptionCode.NOCONTAINEROUTPUT, port, location)
        }
      }
    }
    true
  }

  override def makeBindingsExplicit(): Boolean = {
    for (child <- children) {
      child match {
        case step: PipelineStep =>
          step.makeBindingsExplicit()
        case _ => Unit
      }
    }
    valid = valid && makeInputBindingsExplicit() && makeOutputBindingsExplicit()
    valid
  }
}
