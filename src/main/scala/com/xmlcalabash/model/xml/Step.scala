package com.xmlcalabash.model.xml

import scala.collection.mutable

class Step(override val parent: Option[Artifact]) extends Artifact(parent) {

  def checkInputs(): Unit = {
    var primary = Option.empty[Input]
    val ports = mutable.HashSet.empty[String]
    for (child <- children) {
      child match {
        case input: Input =>
          if (ports.contains(input.port.get)) {
            throw new XmlPipelineException("dupport", s"Duplicate port: ${input.port.get}")
          }
          ports += input.port.get

          if (input.primary) {
            if (primary.isDefined) {
              throw new XmlPipelineException("dupprimary",
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
  }

  def checkOutputs(): Unit = {
    var primary = Option.empty[Output]
    val ports = mutable.HashSet.empty[String]
    for (child <- children) {
      child match {
        case output: Output =>
          if (ports.contains(output.port.get)) {
            throw new XmlPipelineException("dupport", s"Duplicate port: ${output.port.get}")
          }
          ports += output.port.get

          if (output.primary) {
            if (primary.isDefined) {
              throw new XmlPipelineException("dupprimary",
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
  }

  def addDeclaredBindings(): Unit = {
    Unit
  }

  override def makeBindingsExplicit(): Boolean = {
    addDeclaredBindings()
    checkInputs()
    checkOutputs()
    true
  }


}
