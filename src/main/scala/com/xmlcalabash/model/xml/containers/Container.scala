package com.xmlcalabash.model.xml.containers

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.xml.datasource.Pipe
import com.xmlcalabash.model.xml.{Artifact, Input, Output, PipelineStep}
import com.xmlcalabash.runtime.XMLCalabashRuntime
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Container(override val config: XMLCalabashRuntime,
                override val parent: Option[Artifact],
                override val stepType: QName) extends PipelineStep(config, parent) {
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
            throw XProcException.xsDupPortName(input.port.get, input.location)
          }
          ports += input.port.get

          if (input.primary.getOrElse(false)) {
            if (primary.isDefined) {
              throw XProcException.xsDupPrimaryPort(input.port.get, primary.get.port.get, input.location)
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

    true
  }

  override def makeOutputPortsExplicit(): Boolean = {
    var primary = Option.empty[Output]
    val ports = mutable.HashSet.empty[String]
    for (child <- children) {
      child match {
        case output: Output =>
          if (ports.contains(output.port.get)) {
            throw XProcException.xsDupPortName(output.port.get, output.location)
          }
          ports += output.port.get

          if (output.primary.getOrElse(false)) {
            if (primary.isDefined) {
              throw XProcException.xsDupPrimaryPort(output.port.get, primary.get.port.get, output.location)
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
        if (primary.get.primary.isEmpty) {
          primary.get.primary = true
        }
      }
    }

    true
  }

  override def makePortsExplicit(): Boolean = {
    var valid = true

    for (child <- children) {
      child match {
        case step: PipelineStep =>
          valid = valid && step.makePortsExplicit()
        case _ => Unit
      }
    }
    valid = valid && makeInputPortsExplicit() && makeOutputPortsExplicit()

    if (valid) {
      for (port <- outputPorts) {
        val out = output(port)
        if (inputPorts.contains(port)) {
          throw XProcException.xsDupPortName(port, out.get.location)
        }
      }
    }

    valid
  }

  override def makeOutputBindingsExplicit(): Boolean = {
    for (port <- outputPorts) {
      val out = output(port).get
      val sources = out.dataSources
      if (sources.isEmpty) {
        val last = lastChildStep

        if (out.primary.get) {
          if (last.isDefined) {
            var output = Option.empty[Output]
            for (oport <- last.get.outputPorts) {
              val oput = last.get.output(oport).get
              if (oput.primary.getOrElse(false)) {
                output = Some(oput)
              }
            }
            if (output.isDefined) {
              val pipe = new Pipe(config, out, last.get.name, output.get.port.get)
              out.addChild(pipe)
            } else {
              throw XProcException.xsUnconnectedOutputPort(name, port, location)
            }
          } else {
            throw XProcException.xsUnconnectedOutputPort(name, port, location)
          }
        } else {
          // secondary outputs can be left hanging; they'll get sinks later
        }
      }
    }
    true
  }

  override def makeBindingsExplicit(): Boolean = {
    var valid = true
    for (child <- children) {
      child match {
        case step: PipelineStep =>
          valid = valid && step.makeBindingsExplicit()
        case _ => Unit
      }
    }

    valid && makeInputBindingsExplicit() && makeOutputBindingsExplicit()
  }

  override def asXML: xml.Elem = {
    dumpAttr("name", _name)
    val nodes = ListBuffer.empty[xml.Node]
    nodes += xml.Text("\n")
    for (child <- children) {
      nodes += child.asXML
      nodes += xml.Text("\n")
    }
    new xml.Elem("p", "container", dump_attr.getOrElse(xml.Null), namespaceScope, false, nodes:_*)
  }
}
