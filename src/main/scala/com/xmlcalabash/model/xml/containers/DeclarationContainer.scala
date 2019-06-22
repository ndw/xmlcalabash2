package com.xmlcalabash.model.xml.containers

import com.xmlcalabash.config.StepSignature
import com.xmlcalabash.model.xml.{Artifact, DeclareStep, Function, Library}
import com.xmlcalabash.runtime.XMLCalabashRuntime
import net.sf.saxon.s9api.QName

import scala.collection.mutable.ListBuffer

class DeclarationContainer(override val config: XMLCalabashRuntime,
                           override val parent: Option[Artifact],
                           override val stepType: QName) extends Container(config, parent, stepType) {
  // FIXME: what about static variables referenced by declared steps?
  protected val _declaredSteps = ListBuffer.empty[DeclareStep]
  protected val _declaredLibraries = ListBuffer.empty[Library]
  protected val _declaredFunctions = ListBuffer.empty[Function]

  def declaredSteps: List[DeclareStep] = _declaredSteps.toList
  def declaredFunctions: List[Function] = _declaredFunctions.toList
  def decalredLibraries: List[Library] = _declaredLibraries.toList

  def declarations: List[Artifact] = {
    val buf = ListBuffer.empty[Artifact]
    buf ++= _declaredSteps
    buf ++= _declaredFunctions
    buf ++= _declaredLibraries
    buf.toList
  }

  override def stepDeclaration(stepType: QName): Option[DeclareStep] = {
    var stepDecl = Option.empty[DeclareStep]
    for (art <- _declaredSteps) {
      art match {
        case decl: DeclareStep =>
          if (decl.declaredType.isDefined && decl.declaredType.get == stepType) {
            stepDecl = Some(decl)
          }
        case _ => Unit
      }
    }

    if (stepDecl.isEmpty && parent.isDefined) {
      stepDecl = parent.get match {
        case decl: DeclarationContainer =>
          decl.stepDeclaration(stepType)
        case _ => None
      }
    }

    stepDecl
  }

  override def stepSignature(stepType: QName): Option[StepSignature] = {
    var stepDecl = stepDeclaration(stepType)

    if (stepDecl.isDefined) {
      Some(stepDecl.get.signature)
    } else {
      if (config.signatures.stepTypes.contains(stepType)) {
        Some(config.signatures.step(stepType))
      } else {
        None
      }
    }
  }

  override def validate(): Boolean = {
    var valid = super.validate()

    /* already done in the parser
    for (art <- _declaredSteps) {
      valid = art.validate() && valid
      if (! art.atomicStep) {
        valid = valid && art.makePortsExplicit()
        valid = valid && art.makePipesExplicit()
        valid = valid && art.makeBindingsExplicit()
      }
    }
    for (art <- _declaredFunctions) {
      valid = art.validate() && valid
    }
    */

    valid
  }

  override protected[xml] def addChild(child: Artifact): Unit = {
    child match {
      case decl: DeclareStep =>
        _declaredSteps += decl
      case lib: Library =>
        _declaredLibraries += lib
      case func: Function =>
        _declaredFunctions += func
      case _ =>
        children += child
    }
  }
}
