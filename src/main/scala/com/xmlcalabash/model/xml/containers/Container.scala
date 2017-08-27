package com.xmlcalabash.model.xml.containers

import com.xmlcalabash.model.exceptions.ModelException
import com.xmlcalabash.model.xml.datasource.Pipe
import com.xmlcalabash.model.xml.{Artifact, Input, Output, ParserConfiguration, PipelineStep}

import scala.collection.mutable

class Container(override val config: ParserConfiguration,
                override val parent: Option[Artifact]) extends PipelineStep(config, parent) {


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
            throw new ModelException("dupport", s"Duplicate port: ${input.port.get}")
          }
          ports += input.port.get

          if (input.primary) {
            if (primary.isDefined) {
              throw new ModelException("dupprimary",
                s"Multiple primary ports: ${input.port.get} and ${primary.get.port.get}")
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
            throw new ModelException("dupport", s"Duplicate port: ${output.port.get}")
          }
          ports += output.port.get

          if (output.primary) {
            if (primary.isDefined) {
              throw new ModelException("dupprimary",
                s"Multiple primary ports: ${output.port.get} and ${primary.get.port.get}")
            }
            primary = Some(output)
          }
        case _ => Unit
      }
    }

    if (primary.isEmpty && (outputPorts.size == 1)) {
      primary = output(outputPorts.head)
      primary.get.primary = true
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
            throw new ModelException("nooutput", "No output binding")
          }
        } else {
          throw new ModelException("nooutput", "No output binding")
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
