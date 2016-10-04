package com.xmlcalabash.core

import com.xmlcalabash.model.xml.{InputDeclaration, Library, OutputDeclaration, StepDeclaration}
import net.sf.saxon.s9api.{Processor, QName, XdmNode}

/**
  * Created by ndw on 10/1/16.
  */
class XProcEngine(val processor: Processor) {
  val stdLibrary = new Library(None)

  var decl: StepDeclaration = _
  var input: InputDeclaration = _
  var output: OutputDeclaration = _

  decl = new StepDeclaration(None)
  decl.stepType = Some(XProcConstants.p_identity)
  input = new InputDeclaration(None, Some("source"))
  input.primary = Some(true)
  input.sequence = Some(true)
  input.addContentType("*/*")
  decl.addInput(input)
  output = new OutputDeclaration(None, Some("result"))
  output.primary = Some(true)
  output.sequence = Some(true)
  decl.addOutput(output)
  stdLibrary.addDeclaredStep(decl)

  decl = new StepDeclaration(None)
  decl.stepType = Some(XProcConstants.p_sink)
  input = new InputDeclaration(None, Some("source"))
  input.primary = Some(true)
  input.sequence = Some(true)
  input.addContentType("*/*")
  decl.addInput(input)
  stdLibrary.addDeclaredStep(decl)

  def findDeclaration(name: QName): Option[StepDeclaration] = {
    var decl: Option[StepDeclaration] = None

    for (item <- stdLibrary.content.get) {
      item match {
        case stepDecl: StepDeclaration =>
          if (stepDecl.stepType.get == name) {
            decl = Some(stepDecl)
          }
      }
    }

    decl
  }

  def staticError(node: Option[XdmNode], msg: String): Unit = {
    println("Static error: " + msg)
  }

  def dynamicError(node: Option[XdmNode], msg: String): Unit = {
    println("Dynamic error:" + msg)
  }

  def dynamicError(throwable: Throwable): Unit = {
    println("Dynamic error:" + throwable.getCause.getMessage)
    throw throwable
  }
}
